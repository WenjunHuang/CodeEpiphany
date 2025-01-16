package com.wenjunhuang.codeepiphany.services.http

import cats.effect.{ Async, Resource }
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import fs2.io.readInputStream
import java.io.IOException
import java.net.HttpCookie
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.{
  Call,
  Callback,
  Headers as OKHeaders,
  MediaType as OKMediaType,
  OkHttpClient,
  Protocol,
  Request as OKRequest,
  RequestBody,
  Response as OKResponse
}
import okio.BufferedSink
import org.http4s.{ Headers, HttpVersion, Method, Request, Response, Status }
import org.http4s.client.Client
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import com.intellij.openapi.diagnostic.Logger

import com.wenjunhuang.codeepiphany.services.http.OkHttpBuilder.*

/** A builder for [[org.http4s.client.Client]] with an OkHttp backend.
  *
  * @define WHYNOSHUTDOWN
  *   It is assumed that the OkHttp client is passed to us as a Resource, or that the caller will shut it down, or that
  *   the caller is comfortable letting OkHttp's resources expire on their own.
  * @param okHttpClient
  *   the underlying OkHttp client.
  */
sealed abstract class OkHttpBuilder[F[_]] private (val okHttpClient: OkHttpClient)(implicit
  val F: Async[F],
  val HttpClientKeeper: HttpClientManager[F],
  val loggerFactory: LoggerFactory[F]
) {
  private val myLogger       = loggerFactory.getLogger
  private val myUnPureLogger = Logger.getInstance(getClass.getName)

  private def invokeCallback(result: Result[F], cb: Result[F] => Unit, dispatcher: Dispatcher[F]): Unit = {
    val f = logTap(result).flatMap(r => F.delay(cb(r)))
    dispatcher.unsafeRunAndForget(f)
  }

  /** Creates the [[org.http4s.client.Client]]
    *
    * The shutdown method on this client is a no-op. $WHYNOSHUTDOWN
    */
  private def create(dispatcher: Dispatcher[F]): Client[F] = Client(run(dispatcher))

  def resource: Resource[F, Client[F]] =
    Dispatcher.parallel[F].flatMap(dispatcher => Resource.make(F.delay(create(dispatcher)))(client => F.unit))

  private def run(dispatcher: Dispatcher[F])(req: Request[F]) = {
    val addCookiesToRequest = req.uri.host
      .map(host => HttpClientKeeper.getCookiesForHost(CIString(host.value)))
      .getOrElse(List.empty[HttpCookie].pure[F])
      .map { cookies => cookies.foldLeft(req)((req, cookie) => req.addCookie(cookie.getName, cookie.getValue)) }

    Resource.suspend(addCookiesToRequest.flatMap { req =>
      F.async[Resource[F, Response[F]]] { cb =>
        F.delay {
          val cancelledSignal = AtomicBoolean(false)
          okHttpClient.newCall(toOkHttpRequest(req, dispatcher)).enqueue(handler(cb, dispatcher, cancelledSignal))

          Some(F.delay { cancelledSignal.set(true) }) // finalizer to cancel the request
        }
      }
    })
  }

  private def handler(
    cb: Result[F] => Unit,
    dispatcher: Dispatcher[F],
    cancelledSignal: AtomicBoolean // a signal that indicates if the request has been cancelled before the callback is invoked
  )(implicit F: Async[F]): Callback =
    new Callback {
      override def onFailure(call: Call, e: IOException): Unit =
        invokeCallback(Left(e), cb, dispatcher)

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
              val body = readInputStream(F.delay(response.body.byteStream()), 1024, false)
              val dispose = F.delay {
                try response.close()
                catch { case _: Throwable => }
              } *> myLogger.trace("Response was closed after resource use")
              Resource[F, Response[F]](
                F.pure(
                  (
                    Response[F](status = s, headers = getHeaders(response), httpVersion = protocol, body = body),
                    dispose
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
          invokeCallback(r, cb, dispatcher)
      }
    }

  private def getHeaders(response: OKResponse): Headers =
    Headers(response.headers().names().asScala.toList.flatMap { k =>
      response.headers().values(k).asScala.map(k -> _)
    })

  private def toOkHttpRequest(req: Request[F], dispatcher: Dispatcher[F])(implicit F: Async[F]): OKRequest = {
    val body = req match {
      case _ if req.isChunked || req.contentLength.isDefined =>
        new RequestBody {
          override def contentType(): OKMediaType =
            req.contentType.map(c => OKMediaType.parse(`Content-Type`.headerInstance.value(c))).orNull

          // OKHttp will override the content-length header set below and always use "transfer-encoding: chunked" unless this method is overriden
          override def contentLength(): Long = req.contentLength.getOrElse(-1L)

          override def writeTo(sink: BufferedSink): Unit = {
            // This has to be synchronous with this method, or else
            // chunks get silently dropped.
            val f = req.body.chunks
              .map(_.toArray)
              .evalScan(sink) { (oldSink, b) =>
                F.delay(oldSink.write(b))
              }
              .compile
              .drain
            dispatcher.unsafeRunSync(f)
          }
        }
      // if it's a GET or HEAD, okhttp wants us to pass null
      case _ if req.method == Method.GET || req.method == Method.HEAD => null
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

  private def logTap(result: Result[F]): F[Either[Throwable, Resource[F, Response[F]]]] =
    (result match {
      case Left(e)  => myLogger.warn(e)("Error in ok call back")
      case Right(_) => Async[F].unit
    }).map(_ => result)
}

/** Builder for a [[org.http4s.client.Client]] with an OkHttp backend */
object OkHttpBuilder {

  /** Creates a builder.
    * @param okHttpClient
    *   the underlying client.
    */
  def fromUnmanaged[F[_]: Async: HttpClientManager: LoggerFactory](okHttpClient: OkHttpClient): OkHttpBuilder[F] =
    new OkHttpBuilder[F](okHttpClient) {}

  private def defaultOkHttpClient[F[_]: Async: LoggerFactory]: Resource[F, OkHttpClient] =
    Resource.make(Async[F].delay(new OkHttpClient()))(shutdown(_))

  private def shutdown[F[_]](client: OkHttpClient)(implicit F: Async[F], loggerFactory: LoggerFactory[F]) =
    val logger = loggerFactory.getLogger
    F.delay(client.dispatcher.executorService().shutdown()).recoverWith { case NonFatal(t) =>
      logger.warn(t)("Unable to shut down dispatcher when disposing of OkHttp client")
    } *> F.delay {
      client.connectionPool().evictAll()
    }.recoverWith { case NonFatal(t) =>
      logger.warn(t)("Unable to evict connection pool when disposing of OkHttp client")
    } *> F.delay {
      if (client.cache() != null)
        client.cache().close()
    }.recoverWith { case NonFatal(t) =>
      logger.warn(t)("Unable to close cache when disposing of OkHttp client")
    }

  private type Result[F[_]] = Either[Throwable, Resource[F, Response[F]]]

}
