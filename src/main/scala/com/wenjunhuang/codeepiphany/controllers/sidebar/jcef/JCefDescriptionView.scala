package com.wenjunhuang.codeepiphany.controllers.sidebar.jcef

import cats.effect.{ IO, Resource, SyncIO }
import cats.syntax.all.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.*
import com.intellij.util.ui.JBUI
import com.wenjunhuang.codeepiphany.controllers.sidebar.DescriptionPresenter
import com.wenjunhuang.codeepiphany.controllers.sidebar.jcef.{ CefLocalRequestHandler, CefStreamResourceHandler }
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.QuestionStorage.QuestionItem
import com.wenjunhuang.codeepiphany.utils.{ intellijIORuntime, Log }
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.parse
import org.apache.commons.io.IOUtils
import org.cef.browser.*
import org.cef.handler.*

import java.io.{ ByteArrayInputStream, File, FileInputStream }
import java.nio.charset.StandardCharsets
import javax.swing.JComponent
import JCefDescriptionView.*
import org.intellij.lang.annotations.Language

class JCefDescriptionView(private val project: Project, private val presenter: DescriptionPresenter) extends Disposable {
  private var questionItem: Option[QuestionItem] = None

  private val myLifeSpanHandler =
    new CefLifeSpanHandlerAdapter {
      override def onBeforePopup(browser: CefBrowser, frame: CefFrame, target_url: String, target_frame_name: String): Boolean = {
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
          .fromAutoCloseable(SyncIO(getClass.getResourceAsStream("resources/descriptionViewer.html")))
          .use { is =>
            SyncIO(IOUtils.toString(is, StandardCharsets.UTF_8))
          },
        questionItem
          .map(_.descriptionFilePath)
          .map { path =>
            Resource
              .fromAutoCloseable(SyncIO(FileInputStream(File(path))))
              .use { is =>
                SyncIO(IOUtils.toString(is, StandardCharsets.UTF_8))
              }
              .handleErrorWith { e =>
                Log.warn(s"Failed to read description file: $path", e)
                SyncIO("")
              }
          }
          .getOrElse(SyncIO(""))
      ).mapN { (template, description) =>
        template.replace(TEMPLATE_PLACEHOLDER, description)
      }.unsafeRunSync()
      CefStreamResourceHandler(ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "text/html", this).some
    }

    requestHandler.addResource(OVERLAY_SCROLLBARS_CSS_PATH) { () =>
      CefStreamResourceHandler(ByteArrayInputStream(JBCefScrollbarsHelper.getOverlayScrollbarsSourceCSS.getBytes(StandardCharsets.UTF_8)), "text/css", this).some
    }

    requestHandler.addResource(OVERLAY_SCROLLBARS_JS_PATH) { () =>
      CefStreamResourceHandler(ByteArrayInputStream(JBCefScrollbarsHelper.getOverlayScrollbarsSourceJS.getBytes(StandardCharsets.UTF_8)), "text/javascript", this).some
    }

    requestHandler.addResource(SCROLLBARS_CSS_PATH) { () =>
      CefStreamResourceHandler(ByteArrayInputStream(JBCefScrollbarsHelper.getOverlayScrollbarStyle.getBytes(StandardCharsets.UTF_8)), "text/css", this).some
    }

    requestHandler.addResource(DOJO_CSS_PATH) { () =>
      for {
        dojo <- questionItem.map(_.dojo)
        is   <- Option(classOf[JCefDescriptionView].getResourceAsStream(cssOfDojo(dojo)))
      } yield CefStreamResourceHandler(is, "text/css", this)
    }

    requestHandler.addResource(DOJO_JS_PATH) { () =>
      for {
        dojo <- questionItem.map(_.dojo)
        is   <- Option(classOf[JCefDescriptionView].getResourceAsStream(jsOfDojo(dojo)))
      } yield CefStreamResourceHandler(is, "text/javascript", this)
    }
    requestHandler
  }

  private val myBrowser = JBCefBrowser
    .createBuilder()
    .setOffScreenRendering(false)
    .setEnableOpenDevToolsMenuItem(true)
    .build()

  private var myState = ViewerState()

  myBrowser.getJBCefClient
    .addRequestHandler(myLocalRequestHandler, myBrowser.getCefBrowser)
    .addLifeSpanHandler(myLifeSpanHandler, myBrowser.getCefBrowser)

  private val myViewerStateJSQuery = JBCefJSQuery.create(myBrowser.asInstanceOf[JBCefBrowserBase])

  Disposer.register(this, myViewerStateJSQuery)
  myViewerStateJSQuery.addHandler { (s: String) =>
    parse(s).flatMap(_.as[ViewerState]) match
      case Right(state) =>
        myState = state
      case _ => ()
    JBCefJSQuery.Response(null)
  }

  def reload(): Unit =
    myBrowser.loadURL(VIEWER_URL)

  def getComponent: JComponent = myBrowser.getComponent

  def getPreferredFocusedComponent: JComponent = myBrowser.getCefBrowser.getUIComponent.asInstanceOf[JComponent]

  def updateCurrentQuestion(item: QuestionItem): Unit =
    questionItem = Some(item)
    reload()

  def setZoom(zoom: Int): Unit = execute(s"setZoom($zoom)")

  def getZoom(): Int = myState.zoom

  def reloadStyles(): Unit = execute("reloadStyles()")

  private def execute(@Language("javascript") script: String): Unit = myBrowser.getCefBrowser.executeJavaScript(script, myBrowser.getCefBrowser.getURL(), 0)

  override def dispose(): Unit = {
    myBrowser.getJBCefClient.removeRequestHandler(myLocalRequestHandler, myBrowser.getCefBrowser)
    myBrowser.getJBCefClient.removeLifeSpanHandler(myLifeSpanHandler, myBrowser.getCefBrowser)
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
  final val DOJO_CSS_PATH               = "/dojo.css"
  final val DOJO_STYLE_URL              = s"$PROTOCOL://$HOST$DOJO_CSS_PATH"
  final val DOJO_JS_PATH                = "/dojo.js"

  def cssOfDojo(dojo: CodeDojo): String = s"resources/${dojo.toString.toLowerCase}.css"
  def jsOfDojo(dojo: CodeDojo): String  = s"resources/${dojo.toString.toLowerCase}.js"

  case class ViewerState(zoom: Int = 100)
}
