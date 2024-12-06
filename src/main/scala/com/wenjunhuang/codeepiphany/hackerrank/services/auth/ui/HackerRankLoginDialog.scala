package com.wenjunhuang.codeepiphany.hackerrank.services.auth.ui

import cats.effect.IO
import cats.effect.std.Queue
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.{ JBLabel, JBScrollPane, JBTextArea }
import com.intellij.ui.content.TabbedPaneContentUI
import com.intellij.ui.content.impl.ContentManagerImpl
import com.intellij.ui.jcef.{ JBCefBrowser, JBCefBrowserBuilder, JBCefClient }
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.controllers.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{ saveAuthentication, AskForLoginResult }
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.jcef.CefRemoteRequestHandler
import com.wenjunhuang.codeepiphany.utils.intellijIORuntime
import fs2.Stream
import org.cef.browser.{ CefBrowser, CefFrame }
import org.cef.callback.CefCookieVisitor
import org.cef.handler.{ CefLoadHandler, CefLoadHandlerAdapter }
import org.cef.misc.BoolRef
import org.cef.network.CefCookie
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank

import java.net.HttpCookie
import javax.swing.{ JComponent, ScrollPaneConstants, SwingConstants }

class HackerRankLoginDialog(private val myProject: Project, private val callback: Either[Throwable, AskForLoginResult] => Unit)
    extends DialogWrapper(myProject, false, DialogWrapper.IdeModalityType.IDE) {
  private val contentManager       = ContentManagerImpl(TabbedPaneContentUI(SwingConstants.TOP), false, myProject, getDisposable)
  private val loginViaCookiePanel  = contentManager.getFactory.createContent(createCookieLoginPanel(), PluginBundle.message("hackerrank.ui.login.viacookie"), true)
  private val loginViaBrowserPanel = contentManager.getFactory.createContent(createBrowserLoginPanel(), PluginBundle.message("hackerrank.ui.login.viabrowser"), true)

  contentManager.addContent(loginViaCookiePanel)
  contentManager.addContent(loginViaBrowserPanel)

  init()
  setTitle(PluginBundle.message("hackerrank.ui.login.title"))

  override def createCenterPanel(): JComponent = contentManager.getComponent

  override def doOKAction(): Unit = super.doOKAction()

  private def createCookieLoginPanel(): JComponent = {
    val cookieText = JBTextArea(10, 20)
    JBScrollPane(cookieText, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  }

  private def showLoginError(): Unit =
    setErrorText(PluginBundle.message("hackerrank.ui.login.error"))

  private def createBrowserLoginPanel(): JComponent = {
    val browser = JBCefBrowserBuilder()
      .setUrl("https://www.hackerrank.com/auth/login")
      .build()
    val requestHandler = CefRemoteRequestHandler(myProject)
    browser.getJBCefClient.addRequestHandler(requestHandler, browser.getCefBrowser)

    @transient
    var queueHandle: Option[Queue[IO, Option[HttpCookie]]] = None

    val program = for {
      queue <- Queue.unbounded[IO, Option[HttpCookie]]
      _     <- IO.delay { queueHandle = Some(queue) }
      streamFromQueue = Stream.fromQueueNoneTerminated(queue)
      _ <- streamFromQueue
        .fold(Nil: List[HttpCookie])((acc, elem) => elem +: acc)
        .evalTap { cookies =>
          cookies.find(_.getName == "remember_hacker_token") match {
            case Some(cookie) =>
              // found a candidate cookie, but need to test it to see if it's valid
              implicit val k: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper
              k.updateCookies(CodeDojo.HackerRank.host, List(cookie)) *> HackerRankApi[IO]().checkLogin().flatMap {
                case true => saveAuthentication[IO](myProject, HackerRank, cookies) *> IO.delay(callback(Right(AskForLoginResult.Done)))
                case false =>
                  IO.unit
              }
            case None =>
              IO.unit
          }
        }
        .compile
        .drain
    } yield ()
    program.unsafeRunAndForget()

    val loadHandler = new CefLoadHandlerAdapter {
      override def onLoadingStateChange(cefBrowser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean): Unit =
        browser.getJBCefCookieManager.getCefCookieManager.visitAllCookies { (cefCookie: CefCookie, count: Int, total: Int, delete: BoolRef) =>
          if cefCookie.domain.contains("hackerrank.com") then
            val cookie = new HttpCookie(cefCookie.name, cefCookie.value)
            cookie.setDomain(cefCookie.domain)
            cookie.setPath(cefCookie.path)

            if count == total - 1 then queueHandle.foreach(q => (q.offer(Some(cookie)) *> q.offer(None)).unsafeRunAndForget())
            else queueHandle.foreach(_.offer(Some(cookie)).unsafeRunAndForget())
          else if count == total - 1 then queueHandle.foreach(q => q.offer(None).unsafeRunAndForget())
          true
        }

      override def onLoadError(browser: CefBrowser, frame: CefFrame, errorCode: CefLoadHandler.ErrorCode, errorText: String, failedUrl: String): Unit =
        super.onLoadError(browser, frame, errorCode, errorText, failedUrl)
    }
    Disposer.register(
      getDisposable,
      { () =>
        browser.getJBCefClient.removeRequestHandler(requestHandler, browser.getCefBrowser)
        browser.dispose()
      }
    )
    JBScrollPane(browser.getComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  }
}

object HackerRankLoginDialog {}
