package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import cats.effect.{ IO, Resource, SyncIO }
import cats.syntax.all.*
import com.intellij.ide.ui.UISettingsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.{ EditorColorsListener, EditorColorsManager }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.*
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.ChallengeStorageItem
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.JCefDescriptionView.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.isDebug
import com.wenjunhuang.codeepiphany.utils.jcef.{ CefLocalRequestHandler, CefStreamResourceHandler }
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.parse
import org.apache.commons.io.IOUtils
import org.cef.browser.*
import org.cef.handler.*
import org.intellij.lang.annotations.Language
import org.typelevel.log4cats.{ Logger, LoggerFactory }

import java.io.{ ByteArrayInputStream, File, FileInputStream }
import java.nio.charset.StandardCharsets
import javax.swing.JComponent

class JCefDescriptionView(
  private val presenter: ChallengeDescriptionPresenter,
  private val styleProvider: ChallengeDescriptionStyleProvider
) extends Disposable {
  private var questionItem: Option[ChallengeStorageItem] = None
  implicit private val logger: Logger[SyncIO] = LoggerFactory[SyncIO].getLogger

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

  private val myLocalRequestHandler = createRequestHandler()

  private def createRequestHandler(): CefLocalRequestHandler = {
    val requestHandler = new CefLocalRequestHandler(PROTOCOL, HOST)
    requestHandler.addResource(VIEW_PATH) { () =>
      val content = (
        Resource
          .fromAutoCloseable(SyncIO.delay(getClass.getResourceAsStream("resources/descriptionViewer.html")))
          .use { is =>
            IOUtils.toString(is, StandardCharsets.UTF_8).pure[SyncIO]
          },
        questionItem
          .map(_.descriptionFilePath)
          .map { path =>
            Resource
              .fromAutoCloseable(SyncIO.delay(FileInputStream(File(path))))
              .use { is =>
                IOUtils.toString(is, StandardCharsets.UTF_8).pure[SyncIO]
              }
              .handleErrorWith { e =>
                Logger[SyncIO].warn(e)(s"Failed to read description file: $path") *> SyncIO("")
              }
          }
          .getOrElse(SyncIO(""))
      ).mapN { (template, description) =>
        template.replace(TEMPLATE_PLACEHOLDER, description)
      }.unsafeRunSync()
      CefStreamResourceHandler(ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "text/html", this).some
    }

    requestHandler.addResource(OVERLAY_SCROLLBARS_CSS_PATH) { () =>
      CefStreamResourceHandler(
        ByteArrayInputStream(JBCefScrollbarsHelper.getOverlayScrollbarsSourceCSS.getBytes(StandardCharsets.UTF_8)),
        "text/css",
        this
      ).some
    }

    requestHandler.addResource(OVERLAY_SCROLLBARS_JS_PATH) { () =>
      CefStreamResourceHandler(
        ByteArrayInputStream(JBCefScrollbarsHelper.getOverlayScrollbarsSourceJS.getBytes(StandardCharsets.UTF_8)),
        "text/javascript",
        this
      ).some
    }

    requestHandler.addResource(SCROLLBARS_CSS_PATH) { () =>
      CefStreamResourceHandler(
        ByteArrayInputStream(JBCefScrollbarsHelper.getOverlayScrollbarStyle.getBytes(StandardCharsets.UTF_8)),
        "text/css",
        this
      ).some
    }

    requestHandler.addResource(DESCRIPTION_CSS_PATH) { () =>
      CefStreamResourceHandler(
        ByteArrayInputStream(
          ChallengeDescriptionStyle.getStyle(styleProvider, questionItem.map(_.dojo)).getBytes(StandardCharsets.UTF_8)
        ),
        "text/css",
        this
      ).some
    }

    requestHandler
  }

  private val myBrowser: JBCefBrowser = createBrowser()

  private def createBrowser(): JBCefBrowser = {
    val builder = JBCefBrowser
      .createBuilder()

    if isDebug then
      builder
        .setOffScreenRendering(false)
        .setEnableOpenDevToolsMenuItem(true)
    builder.build()
  }
  private var myState = ViewerState()

  myBrowser.getJBCefClient
    .addRequestHandler(myLocalRequestHandler, myBrowser.getCefBrowser)
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
        execute(s"sendInfo = function(info_text) {${myViewerStateJSQuery.inject("info_text")};}")
  }

  myBrowser.getJBCefClient.addLoadHandler(myLoadHandler, myBrowser.getCefBrowser)

  private val busConnection = ApplicationManager.getApplication.getMessageBus.connect(this)
  busConnection.subscribe(EditorColorsManager.TOPIC, scheme => reloadStyles())
  busConnection.subscribe(UISettingsListener.TOPIC, uiSettings => reloadStyles())

  def reload(): Unit =
    myBrowser.loadURL(VIEWER_URL)

  def uiComponent: JComponent = myBrowser.getComponent

  def preferredFocusedComponent: JComponent = myBrowser.getCefBrowser.getUIComponent.asInstanceOf[JComponent]

  def updateCurrentQuestion(item: ChallengeStorageItem): Unit =
    questionItem = Some(item)
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
    myBrowser.getCefBrowser.executeJavaScript(script, myBrowser.getCefBrowser.getURL(), 0)

  override def dispose(): Unit = {
    myBrowser.getJBCefClient.removeRequestHandler(myLocalRequestHandler, myBrowser.getCefBrowser)
    myBrowser.getJBCefClient.removeLifeSpanHandler(myLifeSpanHandler, myBrowser.getCefBrowser)
    myBrowser.getJBCefClient.removeLoadHandler(myLoadHandler, myBrowser.getCefBrowser)
    Disposer.dispose(myBrowser)
  }
}

object JCefDescriptionView {
  final val TEMPLATE_PLACEHOLDER        = "{{questionDescription}}"
  final val PROTOCOL                    = "http"
  final val HOST                        = "localhost"
  final val VIEW_PATH                   = "/descriptionViewer.html"
  final val VIEWER_URL                  = s"$PROTOCOL://$HOST$VIEW_PATH"
  final val OVERLAY_SCROLLBARS_CSS_PATH = "/overlayscrollbars.css"
  final val OVERLAY_SCROLLBARS_JS_PATH  = "/overlayscrollbars.browser.es6.js"
  final val SCROLLBARS_CSS_PATH         = "/scrollbars.css"
  final val SCROLLBARS_STYLE_URL        = s"$PROTOCOL://$HOST$SCROLLBARS_CSS_PATH"
  final val DESCRIPTION_CSS_PATH        = "/descriptionStyle.css"
  final val DESCRIPTION_STYLE_URL       = s"$PROTOCOL://$HOST$DESCRIPTION_CSS_PATH"
  final val DOJO_CSS_PATH               = "/dojo.css"
  final val DOJO_STYLE_URL              = s"$PROTOCOL://$HOST$DOJO_CSS_PATH"

  case class ViewerState(zoom: Double = 100.0, canZoomIn: Boolean = true, canZoomOut: Boolean = true)
}
