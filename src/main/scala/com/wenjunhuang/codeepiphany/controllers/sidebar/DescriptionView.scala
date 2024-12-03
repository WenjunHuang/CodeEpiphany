package com.wenjunhuang.codeepiphany.controllers.sidebar

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.{ JBCefApp, JBCefBrowser, JBCefBrowserBase, JCEFHtmlPanel }
import com.intellij.util.ui.JBUI
import com.wenjunhuang.codeepiphany.controllers.http.HttpClientService
import com.wenjunhuang.codeepiphany.controllers.sidebar.DescriptionView.*
import com.wenjunhuang.codeepiphany.utils.intellijIORuntime
import fs2.Stream
import org.cef.browser.*
import org.cef.callback.CefCallback
import org.cef.handler.*
import org.cef.misc.{ BoolRef, IntRef, StringRef }
import org.cef.network.{ CefRequest, CefResponse, CefURLRequest }
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{ `Content-Length`, `Content-Type` }
import org.http4s.{ Headers, Method, Status, Uri }

import java.nio.ByteBuffer
import java.util as ju
import java.util.concurrent.ArrayBlockingQueue
import javax.swing.JComponent
import scala.jdk.CollectionConverters.*

class DescriptionView(private val project: Project, private val presenter: DescriptionPresenter) extends Disposable {
  private val requestHandler = new CefRequestHandlerAdapter {
//    override def onBeforeBrowse(browser: CefBrowser, frame: CefFrame, request: CefRequest, user_gesture: Boolean, is_redirect: Boolean): Boolean = {
//      val requestUrl = request.getURL
//      if currentUrl.exists(requestUrl.startsWith) then false
//      else if !user_gesture then false
//      else
//        presenter.userClickedLink[IO](requestUrl).unsafeRunAndForget()
//        true
//    }

    override def getResourceRequestHandler(
        browser: CefBrowser,
        frame: CefFrame,
        request: CefRequest,
        isNavigation: Boolean,
        isDownload: Boolean,
        requestInitiator: String,
        disableDefaultHandling: BoolRef
    ): CefResourceRequestHandler = {
      val requestUrl = request.getURL
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
          if request.getURL == null then null
          else
            new CefResourceHandlerAdapter with Http4sClientDsl[IO] {
              override def processRequest(request: CefRequest, callback: CefCallback): Boolean = {
                val httpClientService = HttpClientService.getInstance(project)
                val h4sRequest        = Method.GET(uri = Uri.unsafeFromString(request.getURL), headers = request.headers)
                httpClientService.client.use { client =>
                  client
                    .stream(h4sRequest)
                    .handleErrorWith(e =>
                      Stream.eval(IO.delay {
                        callback.cancel()
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
                }.unsafeRunAndForget()

                true
              }

              override def getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef): Unit = {
                response.setStatus(status.code)
                headers.headers.map(h => (h.name.toString, h.value)).toMap.foreach { case (k, v) => response.setHeaderByName(k, v, true) }
                response.setMimeType(headers.get[`Content-Type`].map(_._1.toString).getOrElse("text/html"))
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

              override def cancel(): Unit =
                super.cancel()
            }

        override def onResourceLoadComplete(browser: CefBrowser, frame: CefFrame, request: CefRequest, response: CefResponse, status: CefURLRequest.Status, receivedContentLength: Long): Unit =
          super.onResourceLoadComplete(browser, frame, request, response, status, receivedContentLength)
      }
    }
  }

  private val lifeSpanHandler =
    new CefLifeSpanHandlerAdapter {
      override def onBeforePopup(browser: CefBrowser, frame: CefFrame, target_url: String, target_frame_name: String): Boolean = {
        presenter.userClickedLink[IO](target_url).unsafeRunAndForget()
        true
      }
    }

  private val jcefHtmlPanel =
    if JBCefApp.isSupported then
      val htmlPanel = JBCefBrowser
        .createBuilder()
        .setEnableOpenDevToolsMenuItem(true)
//        .setUrl("https://www.bilibili.com/")
        .build()
      htmlPanel.setErrorPage { (errorCode, errorText, failedUrl) =>
        if errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED then null
        else JBCefBrowserBase.ErrorPage.DEFAULT.create(errorCode, errorText, failedUrl)
      }
      htmlPanel.getJBCefClient
        .addRequestHandler(
          requestHandler,
          htmlPanel.getCefBrowser
        )
//        .addLifeSpanHandler(
//          lifeSpanHandler,
//          htmlPanel.getCefBrowser
//        )
      Some(htmlPanel)
    else None

  @volatile
  private var currentUrl: Option[String] = None

  def loadUrl(url: String): Unit = {
    currentUrl = Some(url)
    jcefHtmlPanel.foreach(_.loadURL(url))
  }

  def getComponent: JComponent =
    jcefHtmlPanel match
      case Some(panel) => JBUI.Panels.simplePanel().addToCenter(panel.getComponent).addToTop(JBLabel("Loading...")).addToBottom(JBLabel("Powered by JCEF"))
      case None        => JBLabel("JCEF is not supported")

  override def dispose(): Unit =
    jcefHtmlPanel.foreach { htmlPanel =>
      htmlPanel.getJBCefClient.removeRequestHandler(requestHandler, htmlPanel.getCefBrowser)
      htmlPanel.getJBCefClient.removeLifeSpanHandler(lifeSpanHandler, htmlPanel.getCefBrowser)
    }
}

object DescriptionView {
  extension (cefRequest: CefRequest) {
    def headers: Headers = {
      val jMap = ju.HashMap[String, String]()
      cefRequest.getHeaderMap(jMap)
      jMap.asScala.foldLeft(Headers.empty) { case (headers, (k, v)) => headers.put(k -> v) }
    }
  }

}
