package com.wenjunhuang.codeepiphany.services.http

import cats.effect.{IO, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import fs2.io.readInputStream
import java.io.IOException
import java.net.HttpCookie
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.{Call, Callback, OkHttpClient, Protocol, RequestBody, Headers as OKHeaders, MediaType as OKMediaType, Request as OKRequest, Response as OKResponse}
import okio.BufferedSink
import org.http4s.{Headers, HttpVersion, Method, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.http4s.headers.{`Content-Type`, Location}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import com.intellij.openapi.diagnostic.Logger

import com.wenjunhuang.codeepiphany.services.http.OkHttpBuilder.*
import com.wenjunhuang.codeepiphany.utils.syntax.*

/** A builder for [[org.http4s.client.Client]] with an OkHttp backend.
  *
  * @define WHYNOSHUTDOWN
  *   It is assumed that the OkHttp client is passed to us as a Resource, or that the caller will shut it down, or that
  *   the caller is comfortable letting OkHttp's resources expire on their own.
  * @param okHttpClient
  *   the underlying OkHttp client.
  */
sealed abstract class OkHttpBuilder private (val okHttpClient: OkHttpClient) {
  private val myLogger       = LoggerFactory.getLogger[IO]
  private val myUnPureLogger = Logger.getInstance(getClass.getName)

  private def invokeCallback(result: Result, cb: Result => Unit): Unit = {
    logTap(result).flatMap(r => IO.delay(cb(r))).unsafeRunAndForget()
  }

  def resource: Resource[IO, Client[IO]] =
    Dispatcher.parallel[IO].flatMap(dispatcher => Resource.make(IO.delay(Client(run)))(_ => IO.unit))

  private def run(req: Request[IO]): Resource[IO, Response[IO]] = {
    val addCookiesToRequest = req.uri.host
      .map(host => HttpClientManager.getCookiesForHost(CIString(host.value)))
      .getOrElse(List.empty[HttpCookie].pure[IO])
      .map { cookies => cookies.foldLeft(req)((req, cookie) => req.addCookie(cookie.getName, cookie.getValue)) }

    Resource
      .suspend(addCookiesToRequest.flatMap { req =>
        IO.async[Resource[IO, Response[IO]]] { cb =>
          IO.delay {
            val cancelledSignal = AtomicBoolean(false)
            okHttpClient.newCall(toOkHttpRequest(req)).enqueue(handler(cb, cancelledSignal))

            IO.delay { cancelledSignal.set(true) }.some // finalizer to cancel the request
          }
        }
      })
      .flatMap { response =>
        Resource.eval(updateCookies(req.uri, response)).flatMap { response =>
          // 禁止了OkHttp自动处理redirect，原因是洛谷的防爬机制会定时更新一个标记，当这个标记cookie过期后会以302回复并返回新的标记cookie
          // 所以如果让OkHttp自己处理，那么它不会更新cookie，会导致一直302下去直到OkHttp自己的重定向嵌套阈值触发异常，但这是不正确的
          // 所以这里需要明确处理redirect，更新cookie以避免这个问题，也提高了问题提交的稳定性
          // TODO: 实现重定向阈值
          response.status.responseClass match
            case Status.Redirection =>
              val newLocation = response.headers.get[Location].get.uri
              // 重定向用get方法，符合浏览器行为
              run(Request(Method.GET, req.uri.resolve(newLocation), headers = req.headers.removePayloadHeaders))
            case _ =>
              Resource.eval(IO.pure(response))
        }
      }
  }

  private def updateCookies(uri: Uri, response: Response[IO]): IO[Response[IO]] = {
    uri.host
      .map(host =>
        HttpClientManager
          .updateCookiesForHost(CIString(host.value), response.cookies.map(c => HttpCookie(c.name, c.content)))
      )
      .traverse(identity) *> IO.pure(response)
  }

  private def handler(
    cb: Result => Unit,
    cancelledSignal: AtomicBoolean // a signal that indicates if the request has been cancelled before the callback is invoked
  ): Callback =
    new Callback {
      override def onFailure(call: Call, e: IOException): Unit =
        invokeCallback(Left(e), cb)

      override def onResponse(call: Call, response: OKResponse): Unit = {
        if cancelledSignal.get() then
          // if the request has been cancelled, we should not invoke the callback
          myUnPureLogger.trace(
            "Request was cancelled before onResponse is called, so close the response and do nothing"
          )
          try response.close()
          catch { case _: Throwable => }
        else
          val protocol = response.protocol() match {
            case Protocol.HTTP_2   => HttpVersion.`HTTP/2`
            case Protocol.HTTP_1_1 => HttpVersion.`HTTP/1.1`
            case Protocol.HTTP_1_0 => HttpVersion.`HTTP/1.0`
            case _                 => HttpVersion.`HTTP/1.1`
          }
          val r = Status
            .fromInt(response.code())
            .map { s =>
              val body = readInputStream(IO.delay(response.body.byteStream()), 1024, false)

              val okhttpResponseDisposer = IO.delay {
                try response.close()
                catch { case _: Throwable => }
              } *> myLogger.trace("Response was closed after resource use")

              Resource[IO, Response[IO]](
                IO.pure(
                  (
                    Response[IO](status = s, headers = getHeaders(response), httpVersion = protocol, body = body),
                    okhttpResponseDisposer
                  )
                )
              )
            }
            .leftMap { t =>
              // we didn't understand the status code, close the body and return a failure
              try response.close()
              catch { case _: Throwable => }
              t
            }
          invokeCallback(r, cb)
      }
    }

  private def getHeaders(response: OKResponse): Headers =
    Headers(response.headers().names().asScala.toList.flatMap { k =>
      response.headers().values(k).asScala.map(k -> _)
    })

  private def toOkHttpRequest(req: Request[IO]): OKRequest = {
    val body = req match {
      // if it's a GET or HEAD, okhttp wants us to pass null
      case _ if req.method == Method.GET || req.method == Method.HEAD => null
      case _ if req.isChunked || req.contentLength.isDefined =>
        new RequestBody {
          override def contentType(): OKMediaType =
            req.contentType.map(c => OKMediaType.parse(`Content-Type`.headerInstance.value(c))).orNull

          // OKHttp will override the content-length header set below and always use "transfer-encoding: chunked" unless this method is overriden
          override def contentLength(): Long = req.contentLength.getOrElse(-1L)

          override def writeTo(sink: BufferedSink): Unit = {
            // This has to be synchronous with this method, or else
            // chunks get silently dropped.
            req.body.chunks
              .map(_.toArray)
              .scan(sink) { (oldSink, b) =>
                oldSink.write(b)
              }
              .compile
              .drain
              .unsafeRunSync()
          }
        }
      // for anything else we can pass a body which produces no output
      case _ =>
        new RequestBody {
          override def contentType(): OKMediaType        = null
          override def writeTo(sink: BufferedSink): Unit = ()
        }
    }

    new OKRequest.Builder()
      .headers(OKHeaders.of(req.headers.headers.map(h => (h.name.toString, h.value)).toMap.asJava))
      .method(req.method.toString(), body)
      .url(req.uri.toString())
      .build()
  }

  private def logTap(result: Result): IO[Either[Throwable, Resource[IO, Response[IO]]]] =
    (result match {
      case Left(e)  => myLogger.warn(e)("Error in ok call back")
      case Right(_) => IO.unit
    }).map(_ => result)
}

/** Builder for a [[org.http4s.client.Client]] with an OkHttp backend */
object OkHttpBuilder {

  /** Creates a builder.
    * @param okHttpClient
    *   the underlying client.
    */
  def fromUnmanaged(okHttpClient: OkHttpClient): OkHttpBuilder =
    new OkHttpBuilder(okHttpClient) {}

  private def shutdown[F[_]](client: OkHttpClient) =
    val logger = LoggerFactory.getLogger[IO]
    IO.delay(client.dispatcher.executorService().shutdown()).recoverWith { case NonFatal(t) =>
      logger.warn(t)("Unable to shut down dispatcher when disposing of OkHttp client")
    } *> IO.delay {
      client.connectionPool().evictAll()
    }.recoverWith { case NonFatal(t) =>
      logger.warn(t)("Unable to evict connection pool when disposing of OkHttp client")
    } *> IO.delay {
      if (client.cache() != null)
        client.cache().close()
    }.recoverWith { case NonFatal(t) =>
      logger.warn(t)("Unable to close cache when disposing of OkHttp client")
    }

  private type Result = Either[Throwable, Resource[IO, Response[IO]]]

}
