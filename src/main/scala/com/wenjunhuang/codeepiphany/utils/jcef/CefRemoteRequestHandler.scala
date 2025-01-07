package com.wenjunhuang.codeepiphany.utils.jcef

import cats.effect.{ IO, Resource }
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.services.http.HttpClientService
import com.wenjunhuang.codeepiphany.utils.*
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.jcef.CefRemoteRequestHandler.createResourceRequestHandler
import fs2.concurrent.SignallingRef
import org.cef.browser.{ CefBrowser, CefFrame }
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.{ BoolRef, IntRef, StringRef }
import org.cef.network.{ CefPostDataElement, CefRequest, CefResponse }
import org.http4s.{ Headers, Method, Status, Uri }
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{ `Content-Length`, `Content-Type` }
import org.typelevel.log4cats.{ Logger, LoggerFactory }

import java.util as ju
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
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
  ): CefResourceRequestHandler = createResourceRequestHandler(project)
}

object CefRemoteRequestHandler {
  def createResourceRequestHandler(project: Project): CefResourceRequestHandler =
    new CefResourceRequestHandlerAdapter {
      override def getResourceHandler(browser: CefBrowser, frame: CefFrame, request: CefRequest): CefResourceHandler =
        new CefResourceHandlerAdapter with Http4sClientDsl[IO] {
          @volatile
          var headers: Headers = Headers.empty
          @volatile
          @volatile
          private var status: Status = Status.Ok
          @volatile
          private var done: Boolean = false
          @volatile
          private var readResponseCallback: Option[CefCallback] = None

          private val signal: SignallingRef[IO, Boolean] = SignallingRef[IO, Boolean](false).unsafeRunSync()

          private val queue: ArrayBlockingQueue[ByteBuffer] = ju.concurrent.ArrayBlockingQueue[ByteBuffer](10)

          private val myLogger: Logger[IO] = LoggerFactory[IO].getLogger

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
                      if body.isEmpty then
                        method.apply(uri = Uri.unsafeFromString(requestUrl), headers = requestHeaders)
                      else method.apply(body = body, uri = Uri.unsafeFromString(requestUrl), headers = requestHeaders)

                    client
                      .stream(h4sRequest)
                      .map { response =>
                        headers = response.headers
                        status = response.status
                        callback.Continue()
                        response
                      }
                      .flatMap(response => response.body.chunkMin(4086))
                      .evalTap(chunk =>
                        IO.blocking {
                          val buffer = chunk.toByteBuffer
                          queue.put(buffer)

                          readResponseCallback.foreach(_.Continue())
                          readResponseCallback = None
                        }
                      )
                      .interruptWhen(signal)
                      .onFinalizeCaseWeak { existCase =>
                        done = true
                        val callback = readResponseCallback
                        readResponseCallback = None
                        existCase match
                          case Resource.ExitCase.Canceled =>
                            callback.foreach(_.cancel())
                            myLogger.info(s"Request ${method.name} $requestUrl is cancelled")
                          case Resource.ExitCase.Errored(e) =>
                            callback.foreach(_.cancel())
                            myLogger.warn(e)(s"Failed to process ${method.name} $requestUrl")
                          case Resource.ExitCase.Succeeded =>
                            callback.foreach(_.Continue())
                            myLogger.info(s"Request ${method.name} $requestUrl is completed")
                      }
                      .compile
                      .drain
                  }
                  .handleErrorWith(e =>
                    myLogger.warn(e)(s"Failed to process request") *>
                      IO.delay(callback.cancel())
                  )
                  .unsafeRunAndForget()

                true

          override def getResponseHeaders(
            response: CefResponse,
            responseLength: IntRef,
            redirectUrl: StringRef
          ): Unit = {
            response.setStatus(status.code)
            headers.headers.map(h => (h.name.toString, h.value)).toMap.foreach { case (k, v) =>
              response.setHeaderByName(k, v, true)
            }
            response.setMimeType(headers.get[`Content-Type`].map(_._1.show).getOrElse("text/html"))
            responseLength.set(headers.get[`Content-Length`].map(_.length).getOrElse(-1L).toInt)
          }

          override def readResponse(
            dataOut: Array[Byte],
            bytesToRead: Int,
            bytesRead: IntRef,
            callback: CefCallback
          ): Boolean =
            queue.peek() match
              case null =>
                bytesRead.set(0)
                if done then false
                else
                  readResponseCallback = Some(callback)
                  true
              case head =>
                val toRead = math.min(head.remaining(), bytesToRead)
                head.get(dataOut, 0, toRead)
                bytesRead.set(toRead)
                if head.remaining() == 0 then queue.poll()

                true
        }
    }
}
