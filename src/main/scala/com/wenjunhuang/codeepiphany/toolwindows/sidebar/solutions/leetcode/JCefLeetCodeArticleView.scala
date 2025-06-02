package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode

import cats.effect.{ IO, SyncIO }
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.parse
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.swing.JComponent
import org.cef.browser.*
import org.cef.handler.*
import org.intellij.lang.annotations.Language
import org.typelevel.log4cats.{ Logger, LoggerFactory }

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.{ EditorColorsListener, EditorColorsManager }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.*

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.WebViewStyleProvider
import com.wenjunhuang.codeepiphany.utils.{ isDebug, ResourceHttpServer }
import com.wenjunhuang.codeepiphany.utils.syntax.*

import JCefLeetCodeArticleView.*
class JCefLeetCodeArticleView(
  private val presenter: ArticleDetailPresenter,
  private val myWebViewStyleProvider: WebViewStyleProvider,
  private val myProject: Project
) extends Disposable {
  private implicit val logger: Logger[SyncIO] = LoggerFactory[SyncIO].getLogger

  private val myBusConnection = ApplicationManager.getApplication.getMessageBus.connect(this)
  myBusConnection.subscribe(EditorColorsManager.TOPIC, _ => reloadStyles())
  myBusConnection.subscribe(LafManagerListener.TOPIC, _ => reloadStyles())

  private val myHttpServer: ResourceHttpServer = ResourceHttpServer("webview", 0)

  @volatile
  private var myArticleContent: Option[(String, CodeDojo)] = None

  private val myLifeSpanHandler =
    new CefLifeSpanHandlerAdapter {
      override def onBeforePopup(
        browser: CefBrowser,
        frame: CefFrame,
        target_url: String,
        target_frame_name: String
      ): Boolean = {
        presenter.userClickedLink[IO](target_url).unsafeRunAndForget()
        true
      }
    }

  myHttpServer.addCustomResponse(
    "/intellijStyle.css",
    { () =>
      myWebViewStyleProvider.baseStyle.getBytes(StandardCharsets.UTF_8)
    },
    "text/css"
  )
  myHttpServer.addTemplateResponse(
    "/leetCodeSolutionArticle/index.html",
    "leetCodeSolutionArticle/index.html",
    "text/html",
    { () =>
      Map(
        ARTICLE_CONTENT ->
          Base64.getEncoder.encodeToString(
            myArticleContent.map(_._1).getOrElse("No article selected 🌟").getBytes(StandardCharsets.UTF_8)
          ),
        GET_IFRAME_URL -> ""
      )
    }
  )
  myHttpServer.start()

  private val myBrowser: JBCefBrowser = createBrowser()

  private def createBrowser(): JBCefBrowser = {
    val builder = JBCefBrowser
      .createBuilder()

    if isDebug then
      builder
        .setOffScreenRendering(false)
        .setEnableOpenDevToolsMenuItem(true)
    else builder.setOffScreenRendering(true)
    builder.build()
  }
  private var myState = ViewerState()

  myBrowser.getJBCefClient
    .addLifeSpanHandler(myLifeSpanHandler, myBrowser.getCefBrowser)

  private val myViewerStateJSQuery = createJSQuery()

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

  private val myLoadHandler = new CefLoadHandlerAdapter {
    override def onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int): Unit =
      if frame.isMain then
        reloadStyles()
        execute(s"""
             |window.showSolutionArticle()
             |""".stripMargin)
        execute(s"window.sendInfo = function(info_text) {${myViewerStateJSQuery.inject("info_text")};}")
  }

  myBrowser.getJBCefClient.addLoadHandler(myLoadHandler, myBrowser.getCefBrowser)

  private def reload(): Unit = {
    val port = myHttpServer.getListeningPort.getOrElse(throw IllegalStateException("Http Server not started"))
    myBrowser.loadURL(
      s"http://localhost:${port}/leetCodeSolutionArticle/index.html" + s"?${System.currentTimeMillis()}"
    )
  }

  def uiComponent: JComponent = myBrowser.getComponent

  def preferredFocusedComponent: JComponent = myBrowser.getCefBrowser.getUIComponent.asInstanceOf[JComponent]

  def setArticleContent(content: Option[(String, CodeDojo)]): Unit =
    myArticleContent = content
    reload()

  def zoom_=(zoom: Double): Unit =
    execute(s"setZoom($zoom)")

  def zoom: Double = myState.zoom

  def zoomIn(): Unit = execute("zoomIn()")

  def zoomOut(): Unit = execute("zoomOut()")

  def canZoomIn: Boolean = myState.canZoomIn

  def canZoomOut: Boolean = myState.canZoomOut

  def actualZoom(): Unit = execute("actualZoom()")

  def reloadStyles(): Unit =
    execute("reloadStyles()")

  def performCopy(): Unit = myBrowser.getCefBrowser.getMainFrame.copy()

  private def execute(@Language("javascript") script: String): Unit =
    myBrowser.getCefBrowser.executeJavaScript(script, myBrowser.getCefBrowser.getURL, 0)

  override def dispose(): Unit = {
    myBrowser.getJBCefClient.removeLifeSpanHandler(myLifeSpanHandler, myBrowser.getCefBrowser)
    myBrowser.getJBCefClient.removeLoadHandler(myLoadHandler, myBrowser.getCefBrowser)
    Disposer.dispose(myBrowser)
    myHttpServer.stop()
  }
}

object JCefLeetCodeArticleView {
  final val ARTICLE_CONTENT = "{{articleContent}}"
  final val GET_IFRAME_URL  = "{{getIframeUrl}}"
  case class ViewerState(zoom: Double = 100.0, canZoomIn: Boolean = true, canZoomOut: Boolean = true)
}
