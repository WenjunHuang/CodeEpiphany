package com.wenjunhuang.codeepiphany.controllers.sidebar.jcef

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.controllers.http.HttpClientService
import com.wenjunhuang.codeepiphany.utils.*
import fs2.Stream
import org.cef.browser.{CefBrowser, CefFrame}
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.{BoolRef, IntRef, StringRef}
import org.cef.network.{CefRequest, CefResponse}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{`Content-Length`, `Content-Type`}
import org.http4s.{Headers, Method, Status, Uri}

import java.nio.ByteBuffer
import java.util as ju
import java.util.concurrent.ArrayBlockingQueue

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

      val queue: ArrayBlockingQueue[ByteBuffer] = ju.concurrent.ArrayBlockingQueue[ByteBuffer](10)

      override def getResourceHandler(browser: CefBrowser, frame: CefFrame, request: CefRequest): CefResourceHandler =
        new CefResourceHandlerAdapter with Http4sClientDsl[IO] {
          override def processRequest(request: CefRequest, callback: CefCallback): Boolean = {
            val httpClientService = HttpClientService.getInstance(project)
            val requestUrl        = request.getURL
            val requestHeaders    = request.headers
            httpClientService.client.use { client =>
              val h4sRequest = Method.GET(uri = Uri.unsafeFromString(requestUrl), headers = requestHeaders)
              client
                .stream(h4sRequest)
                .handleErrorWith(e =>
                  Stream.eval(IO.delay {
                    callback.cancel()
                    Log.warn(s"Failed to fetch $requestUrl", e)
                  }) *> Stream.empty
                )
                .evalTap(response =>
                  IO.delay {
                    headers = response.headers
                    status = response.status
                    callback.Continue()
                  }
                )
                .flatMap(response => response.body.chunks)
                .evalTap(chunk =>
                  IO.interruptible {
                    val buffer = ByteBuffer
                      .allocate(chunk.size)
                      .put(chunk.toArray)
                      .flip()
                    queue.put(buffer)

                    readResponseCallback.foreach(_.Continue())
                    readResponseCallback = None
                  }
                )
                .compile
                .drain
                .flatMap(_ =>
                  IO.delay {
                    done = true
                    readResponseCallback.foreach(_.Continue())
                  }
                )
                .handleError(_ =>
                  IO.delay {
                    done = true
                    readResponseCallback.foreach(_.Continue())
                  }
                )
            }.handleErrorWith { e =>
              IO.delay {
                callback.cancel()
                Log.warn(s"Failed to fetch $requestUrl", e)
              }
            }.unsafeRunAndForget()()

            true
          }

          override def getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef): Unit = {
            response.setStatus(status.code)
            headers.headers.map(h => (h.name.toString, h.value)).toMap.foreach { case (k, v) => response.setHeaderByName(k, v, true) }
            response.setMimeType(headers.get[`Content-Type`].map(_._1.show).getOrElse("text/html"))
            responseLength.set(headers.get[`Content-Length`].map(_.length).getOrElse(-1L).toInt)
          }

          override def readResponse(dataOut: Array[Byte], bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean =
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
