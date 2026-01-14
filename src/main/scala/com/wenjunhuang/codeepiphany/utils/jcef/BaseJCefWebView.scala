package com.wenjunhuang.codeepiphany.utils.jcef

import cats.effect.{IO, SyncIO}

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.{EditorColorsListener, EditorColorsManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.*

import com.wenjunhuang.codeepiphany.services.WebViewStyleProvider
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.{isDebug, ResourceHttpServer}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.parse
import org.cef.browser.*
import org.cef.handler.*
import org.cef.network.CefRequest
import org.http4s.Headers
import org.intellij.lang.annotations.Language
import org.typelevel.log4cats.{Logger, LoggerFactory}
import javax.swing.JComponent

import com.wenjunhuang.codeepiphany.utils.jcef.BaseJCefWebView.createDefaultBrowser

/** Abstract base class for JCef-based web views. Provides common functionality for browser setup, event handling, and
  * resource management.
  *
  * @param styleProvider
  *   provides CSS styling for the web view
  * @param myProject
  *   the IntelliJ project instance
  * @param resourcePath
  *   the base path for HTTP server resources (e.g., "webview")
  */
abstract class BaseJCefWebView(
  protected val styleProvider: WebViewStyleProvider,
  protected val myProject: Project,
  protected val resourcePath: String
) extends Disposable {

  import BaseJCefWebView.ViewerState

  protected implicit val logger: Logger[SyncIO] = LoggerFactory[SyncIO].getLogger

  // Message bus connection for theme/style changes
  private val myBusConnection = ApplicationManager.getApplication.getMessageBus.connect(this)
  myBusConnection.subscribe(EditorColorsManager.TOPIC, _ => reloadStyles())
  myBusConnection.subscribe(LafManagerListener.TOPIC, _ => reloadStyles())

  // HTTP server for serving resources
  protected val myHttpServer: ResourceHttpServer = ResourceHttpServer(resourcePath, 0)

  // Browser and state management
  private val myBrowser: JBCefBrowser = createDefaultBrowser()
  protected var myState: ViewerState  = ViewerState()
  private val myViewerStateJSQuery    = createJSQuery()

  // Event handlers
  private val myLoadHandler = new CefLoadHandlerAdapter {
    override def onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int): Unit =
      if frame.isMain then
        reloadStyles()
        execute(s"window.sendInfo = function(info_text) {${myViewerStateJSQuery.inject("info_text")};}")
  }

  private val myRequestHandlerAdapter = new CefRemoteRequestHandler(myProject, requestFilter, createHeaders) {
    override def onBeforeBrowse(
      browser: CefBrowser,
      frame: CefFrame,
      request: CefRequest,
      user_gesture: Boolean,
      is_redirect: Boolean
    ): Boolean = {
      if frame.isMain && user_gesture && !is_redirect then
        onUserClickedLink(request.getURL)
        true
      else false
    }
  }

  private val myLifeSpanHandler = new CefLifeSpanHandlerAdapter {
    override def onBeforePopup(
      browser: CefBrowser,
      frame: CefFrame,
      target_url: String,
      target_frame_name: String
    ): Boolean = {
      true
    }
  }

  // Initialize the browser client with handlers
  myBrowser.getJBCefClient
    .addLoadHandler(myLoadHandler, myBrowser.getCefBrowser)
    .addRequestHandler(myRequestHandlerAdapter, myBrowser.getCefBrowser)
    .addLifeSpanHandler(myLifeSpanHandler, myBrowser.getCefBrowser)

  // Abstract methods that subclasses must implement
  protected def setupHttpServer(): Unit
  protected def onUserClickedLink(url: String): Unit
  protected def getIndexPath: String

  // Initialize HTTP server
  setupHttpServer()
  myHttpServer.start()



  private def createJSQuery(): JBCefJSQuery = {
    val jsQuery = JBCefJSQuery.create(myBrowser.asInstanceOf[JBCefBrowserBase])

    Disposer.register(this, jsQuery)
    jsQuery.addHandler { (s: String) =>
      parse(s).flatMap(_.as[ViewerState]) match
        case Right(state) =>
          myState = state
        case _ =>
          ()
      JBCefJSQuery.Response(null)
    }
    jsQuery
  }

  protected def reload(): Unit = {
    val port = myHttpServer.getListeningPort.getOrElse(throw IllegalStateException("Http Server not started"))
    myBrowser.loadURL(s"http://localhost:${port}/${getIndexPath}" + s"?${System.currentTimeMillis()}")
  }

  // Public API methods
  def uiComponent: JComponent = myBrowser.getComponent

  def preferredFocusedComponent: JComponent = myBrowser.getCefBrowser.getUIComponent.asInstanceOf[JComponent]

  def zoom_=(zoom: Double): Unit = execute(s"setZoom($zoom)")

  def zoom: Double = myState.zoom

  def zoomIn(): Unit = execute("zoomIn()")

  def zoomOut(): Unit = execute("zoomOut()")

  def canZoomIn: Boolean = myState.canZoomIn

  def canZoomOut: Boolean = myState.canZoomOut

  def actualZoom(): Unit = execute("actualZoom()")

  def reloadStyles(): Unit = execute("reloadStyles()")

  def performCopy(): Unit = myBrowser.getCefBrowser.getMainFrame.copy()

  protected def requestFilter(frame: CefFrame, req: CefRequest): Boolean = true
  protected def createHeaders(request: CefRequest): IO[Headers]          = IO.pure(Headers.empty)

  protected def execute(@Language("javascript") script: String): Unit =
    myBrowser.getCefBrowser.executeJavaScript(script, myBrowser.getCefBrowser.getURL, 0)

  override def dispose(): Unit = {
    myBrowser.getJBCefClient.removeRequestHandler(myRequestHandlerAdapter, myBrowser.getCefBrowser)
    myBrowser.getJBCefClient.removeLifeSpanHandler(myLifeSpanHandler, myBrowser.getCefBrowser)
    myBrowser.getJBCefClient.removeLoadHandler(myLoadHandler, myBrowser.getCefBrowser)
    Disposer.dispose(myBrowser)
    myHttpServer.stop()
  }
}

object BaseJCefWebView {
  case class ViewerState(zoom: Double = 100.0, canZoomIn: Boolean = true, canZoomOut: Boolean = true)

  def createDefaultBrowser(): JBCefBrowser = {
    val builder = JBCefBrowser.createBuilder()

    if isDebug then
      builder
        .setOffScreenRendering(false)
        .setEnableOpenDevToolsMenuItem(true)
    else builder.setOffScreenRendering(true)

    builder.build()
  }
}
