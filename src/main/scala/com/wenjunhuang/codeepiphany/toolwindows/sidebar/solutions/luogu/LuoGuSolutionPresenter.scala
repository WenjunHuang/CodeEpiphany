package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.luogu

import cats.effect.IO
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.{ JBCefBrowser, JBCefCookie, JBCefScrollbarsHelper }
import com.intellij.util.ui.components.BorderLayoutPanel
import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.LuoGu
import com.wenjunhuang.codeepiphany.model.newtypes.ChallengeId
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.services.{ AuthService, ChallengeRepository, WebViewStyleProvider }
import com.wenjunhuang.codeepiphany.utils.isDebug
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView
import org.cef.browser.CefBrowser
import org.cef.handler.CefLoadHandlerAdapter
import org.typelevel.log4cats.LoggerFactory

import javax.swing.{ JComponent, SwingConstants }
import scala.jdk.OptionConverters.*

class LuoGuSolutionPresenter(private val myChallengeId: ChallengeId, private val myProject: Project)
    extends Disposable {
  private val myLogger  = LoggerFactory.getLogger[IO]
  private val myView    = new BorderLayoutPanel()
  private val myBrowser = createBrowser()

  Disposer.register(myProject, this)

  initialize()

  private def initialize(): Unit = {
    if (AuthService.getInstance(myProject).isLoggedIn(LuoGu)) {
      ChallengeRepository
        .getInstance(myProject)
        .getDSLContextResource
        .use { dslContext =>
          IO.delay {
            dslContext
              .select(CHALLENGE.DOJOID)
              .from(CHALLENGE)
              .where(CHALLENGE.ID.eq(myChallengeId.value))
              .fetchOptional()
              .toScala
              .map(_.value1())
              .getOrElse(throw new NoSuchElementException(s"No challenge found for ID: ${myChallengeId.value}"))
          }.handleErrorWith { error =>
            IO.delay {
              myView.addToCenter(
                new JBLabel(
                  s"Error fetching challenge slug: ${error.getMessage}",
                  AnimatedIcon.Default.INSTANCE,
                  SwingConstants.CENTER
                )
              )
            }.evalOnEDTDefault() *> IO.raiseError(error)
          }
        }
        .flatMap { pid =>
          HttpClientManager
            .getCookiesForHost(CodeDojo.LuoGu.domain)
            .map { cookies =>
              cookies.foreach { cookie =>
                val jbcefCookie =
                  JBCefCookie(cookie.getName, cookie.getValue, CodeDojo.LuoGu.domain.toString, "/", true, false)
                myBrowser.getJBCefCookieManager
                  .setCookie(s"https://www.luogu.com.cn/problem/solution/${pid}", jbcefCookie)
                  .cancel(true)
              }
            }
            .void
            .recoverWith { case e: Throwable =>
              myLogger.error(e)("Error when set cookies ") *> IO.unit
            } >> IO.delay {
            myBrowser.getJBCefClient
              .addLoadHandler(
                new CefLoadHandlerAdapter {
                  override def onLoadingStateChange(
                    browser: CefBrowser,
                    isLoading: Boolean,
                    canGoBack: Boolean,
                    canGoForward: Boolean
                  ): Unit = {
                    browser.executeJavaScript(
                      // language=javascript
                      s"""
                        |if (!document.getElementById('ce-style') && document.head){
                        |  try{
                        |
                        |  const styleElement = document.createElement('style')
                        |  styleElement.id = 'ce-style';
                        |  styleElement.innerHTML = `
                        |    .main-container {
                        |      margin:auto !important;
                        |    }
                        |    .header-layout{
                        |    min-height: 0px !important;
                        |    }
                        |    .side, .top-bar, .sidebar, .lfe-h1 ~ * , footer, span.right {
                        |    display: none !important;
                        |    }
                        |    `;
                        |  document.head.appendChild(styleElement);
                        |}catch(ignored){
                        |  console.log(ignored)
                        |}
                        |}
                        |""".stripMargin,
                      myBrowser.getCefBrowser.getURL,
                      0
                    )
                  }
                },
                myBrowser.getCefBrowser
              )
            myView.removeAll()
            myView.addToCenter(myBrowser.getComponent)
            myBrowser.loadURL(s"https://www.luogu.com.cn/problem/solution/${pid}")
          }.evalOnEDTAny()
        }
        .unsafeRunAndForget()
    } else {
      myView.addToCenter(new UnauthenticatedView(CodeDojo.LuoGu))
    }
  }

  private def createBrowser(): JBCefBrowser = {
    val builder = JBCefBrowser.createBuilder()

    if isDebug then
      builder
        .setOffScreenRendering(false)
        .setEnableOpenDevToolsMenuItem(true)
    else builder.setOffScreenRendering(true)

    builder.build()
  }

  override def dispose(): Unit = {
    myBrowser.dispose()
  }

  def getView: JComponent = myView
}
