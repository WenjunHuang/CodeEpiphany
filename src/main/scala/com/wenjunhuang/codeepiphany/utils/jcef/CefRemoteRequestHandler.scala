package com.wenjunhuang.codeepiphany.utils.jcef

import cats.effect.{IO, Resource, SyncIO}
import cats.effect.std.Queue
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.services.http.HttpClientService
import com.wenjunhuang.codeepiphany.utils.*
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.cef.browser.{CefBrowser, CefFrame}
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.{BoolRef, IntRef, StringRef}
import org.cef.network.{CefPostDataElement, CefRequest, CefResponse}
import org.http4s.{Headers, Method, Status, Uri}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{`Content-Length`, `Content-Type`}
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.util as ju
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.*

class CefRemoteRequestHandler(private val project: Project) extends CefRequestHandlerAdapter {
  override def getResourceRequestHandler(
      browser: CefBrowser,
      frame: CefFrame,
      request: CefRequest,
      isNavigation: Boolean,
      isDownload: Boolean,
      requestInitiator: String,
      disableDefaultHandling: BoolRef
  ): CefResourceRequestHandler =
    new CefResourceRequestHandlerAdapter {
      @volatile
      var headers: Headers = Headers.empty
      @volatile
      var status: Status = Status.Ok
      @volatile
      var done: Boolean = false
      @volatile
      var readResponseCallback: Option[CefCallback] = None

      @volatile
      var debugRequestUrl: Option[String] = None

      val debugReadBytes: AtomicInteger = new AtomicInteger(0)

      val signal                                = SignallingRef[IO, Boolean](false).unsafeRunSync()
      val queue: ArrayBlockingQueue[ByteBuffer] = ju.concurrent.ArrayBlockingQueue[ByteBuffer](10)

      override def getResourceHandler(browser: CefBrowser, frame: CefFrame, request: CefRequest): CefResourceHandler =
        new CefResourceHandlerAdapter with Http4sClientDsl[IO] {
          private val logger: Logger[IO]           = LoggerFactory[IO].getLogger
          private val loggerSyncIO: Logger[SyncIO] = LoggerFactory[SyncIO].getLogger

          override def processRequest(request: CefRequest, callback: CefCallback): Boolean =
            Method.fromString(request.getMethod) match
              case Left(e) => callback.cancel(); false
              case Right(method) =>
                val requestUrl     = request.getURL
                val requestHeaders = request.headers
                val body = request.getPostData match
                  case null => Vector.empty
                  case b =>
                    val bodyVector = new ju.Vector[CefPostDataElement](b.getElementCount)
                    b.getElements(bodyVector)
                    bodyVector.asScala.toVector
                HttpClientService
                  .getInstance(project)
                  .http4sClient
                  .use { client =>
                    val h4sRequest =
                      if body.isEmpty then method.apply(uri = Uri.unsafeFromString(requestUrl), headers = requestHeaders)
                      else method.apply(body = body, uri = Uri.unsafeFromString(requestUrl), headers = requestHeaders)

                    client
                      .stream(h4sRequest)
                      .handleErrorWith(e =>
                        Stream.eval(
                          logger.warn(e)(s"Failed to connect $requestUrl") *>
                            IO.delay(callback.cancel())
                        ) *> Stream.empty
                      )
                      .evalTap(response =>
                        (if requestUrl.contains("hrcdn.net") && requestUrl.contains("mp4") then
                           debugRequestUrl = Some(requestUrl)
                           logger.info(s"Get partial mp4 data with length : ${response.headers.get[`Content-Length`].map(_.length).getOrElse(-1L)}")
                         else IO.unit)
                        *> IO.delay {
                          headers = response.headers
                          status = response.status
                          callback.Continue()
                        }
                      )
                      .flatMap(response => response.body.chunkMin(4086))
                      .evalTap(chunk =>
                        IO.interruptible {
                          val buffer = chunk.toByteBuffer
                          queue.put(buffer)

                          readResponseCallback.foreach(_.Continue())
                          readResponseCallback = None
                        }
                      )
                      .interruptWhen(signal)
                      .onFinalizeCase {
                        case Resource.ExitCase.Canceled =>
                          logger.info(s"Request $requestUrl is cancelled")
                        case _ => IO.unit
                      }
                      .compile
                      .drain
                      .flatMap(_ =>
                        IO.delay {
                          done = true
                          readResponseCallback.foreach(_.Continue())
                        }
                      )
                      .handleError(e =>
                        logger.warn(e)(s"Failed to processing $requestUrl") *>
                          IO.delay {
                            done = true
                            readResponseCallback.foreach(_.cancel())
                          }
                      )
                  }.unsafeRunAndForget()

                true

          override def getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef): Unit = {
            response.setStatus(status.code)
            headers.headers.map(h => (h.name.toString, h.value)).toMap.foreach { case (k, v) => response.setHeaderByName(k, v, true) }
            response.setMimeType(headers.get[`Content-Type`].map(_._1.show).getOrElse("text/html"))
            responseLength.set(headers.get[`Content-Length`].map(_.length).getOrElse(-1L).toInt)

            if debugRequestUrl.exists(url => url.contains("hrcdn.net") && url.contains("mp4")) then loggerSyncIO.info(s"Response headers: $headers, response length:${responseLength}").unsafeRunSync()
          }

          override def cancel(): Unit =
            signal.set(true).unsafeRunAndForget()

          override def readResponse(dataOut: Array[Byte], bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean =
            queue.peek() match
              case null =>
                bytesRead.set(0)
                if done then
                  if debugRequestUrl.exists(url => url.contains("hrcdn.net") && url.contains("mp4")) then
                    val total = debugReadBytes.get()
                    loggerSyncIO.info(s"Finish reading $total bytes").unsafeRunSync()
                  false
                else
                  readResponseCallback = Some(callback)
                  true
              case head =>
                val toRead = math.min(head.remaining(), bytesToRead)
                head.get(dataOut, 0, toRead)
                bytesRead.set(toRead)
                if debugRequestUrl.exists(url => url.contains("hrcdn.net") && url.contains("mp4")) then
                  debugReadBytes.addAndGet(toRead)
                  loggerSyncIO.info(s"Reading $toRead bytes, total read ${debugReadBytes.get()} bytes").unsafeRunSync()
                if head.remaining() == 0 then queue.poll()

                true
        }
    }
}
