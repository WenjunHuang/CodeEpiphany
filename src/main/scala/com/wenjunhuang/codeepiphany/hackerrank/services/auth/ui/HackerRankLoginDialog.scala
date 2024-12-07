package com.wenjunhuang.codeepiphany.hackerrank.services.auth.ui

import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.all.*
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{ ComponentValidator, DialogWrapper }
import com.intellij.openapi.util.Disposer
import com.intellij.ui.{ AnimatedIcon, DocumentAdapter }
import com.intellij.ui.components.{ JBScrollPane, JBTextArea }
import com.intellij.ui.content.impl.ContentManagerImpl
import com.intellij.ui.content.{ ContentManagerEvent, ContentManagerListener, TabbedPaneContentUI }
import com.intellij.ui.jcef.{ JBCefBrowser, JBCefBrowserBuilder }
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.controllers.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{ saveAuthentication, validateUserCookieAndTestLogin, AskForLoginResult }
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.utils.jcef.CefRemoteRequestHandler
import com.wenjunhuang.codeepiphany.utils.implicits.*
import fs2.Stream
import org.cef.browser.{ CefBrowser, CefFrame }
import org.cef.callback.CefCookieVisitor
import org.cef.handler.{ CefLoadHandler, CefLoadHandlerAdapter }
import org.cef.misc.BoolRef
import org.cef.network.CefCookie

import java.awt.event.ActionEvent
import java.net.HttpCookie
import javax.swing.event.DocumentEvent
import javax.swing.*

class HackerRankLoginDialog(private val myProject: Project, private val callback: Either[Throwable, AskForLoginResult] => Unit)
    extends DialogWrapper(myProject, false, DialogWrapper.IdeModalityType.IDE) {
  private val myContentManager = ContentManagerImpl(TabbedPaneContentUI(SwingConstants.TOP), false, myProject, getDisposable)

  private val myCookieText = JBTextArea(10, 20)
  myCookieText.setLineWrap(true)
  myCookieText.getDocument.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit =
      e.getDocument.getLength match
        case 0 => setOKActionEnabled(false)
        case _ => setOKActionEnabled(true)
  })

  private val myLoginViaCookiePanel = myContentManager.getFactory.createContent(
    JBScrollPane(myCookieText, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
    PluginBundle.message("hackerrank.ui.login.viacookie"),
    true
  )
  private val myLoginBrowser = createBrowserLoginPanel()
  private val myLoginViaBrowserPanel = myContentManager.getFactory.createContent(
    JBScrollPane(myLoginBrowser.getComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
    PluginBundle.message("hackerrank.ui.login.viabrowser"),
    true
  )
  implicit private val myHttpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper

  private val myOkAction: OkAction = new OkAction {
    override def doAction(e: ActionEvent): Unit = {
      val text = myCookieText.getText

      IO.delay {
        myOkAction.setEnabled(false)
        myOkAction.putValue(Action.SMALL_ICON, AnimatedIcon.Default.INSTANCE)
        myOkAction.putValue(Action.NAME, PluginBundle.message("hackerrank.ui.login.validating"))
      }.evalOn(intellijUIContext)
        .flatMap(_ => validateUserCookieAndTestLogin[IO](myProject, HackerRank, text))
        .flatMap {
          case true  => IO.delay(callback(Right(AskForLoginResult.Done)))
          case false => throw Exception(PluginBundle.message("hackerrank.ui.login.error"))
        }
        .recoverWith(e =>
          IO.delay {
            showLoginError(e.getMessage)
            myOkAction.setEnabled(true)
            myOkAction.putValue(Action.SMALL_ICON, null)
            myOkAction.putValue(Action.NAME, PluginBundle.message("hackerrank.ui.login.ok"))
          }
        )
        .unsafeRunAndForget()
    }
  }

  myContentManager.addContent(myLoginViaCookiePanel)
  myContentManager.addContent(myLoginViaBrowserPanel)
  myContentManager.addContentManagerListener(new ContentManagerListener {
    override def selectionChanged(event: ContentManagerEvent): Unit =
      if event.getContent == myLoginViaCookiePanel then getButton(myOkAction).setVisible(true)
      else getButton(myOkAction).setVisible(false)
  })

  init()
  setTitle(PluginBundle.message("hackerrank.ui.login.title"))
  setOKButtonText(PluginBundle.message("hackerrank.ui.login.ok"))

  override def createCenterPanel(): JComponent = myContentManager.getComponent

  override def doCancelAction(): Unit =
    callback(Right(AskForLoginResult.Cancelled))
    super.doCancelAction()

  override def getOKAction: Action = myOkAction

  override def createActions(): Array[Action] = {
    val helpAction: Action = new AbstractAction(PluginBundle.message("hackerrank.ui.login.help")) {
      override def actionPerformed(e: ActionEvent): Unit =
        BrowserUtil.browse("https://www.hackerrank.com/auth/login")
    }
    Array(helpAction, myOkAction, getCancelAction)
  }

  private def showLoginError(msg: String): Unit =
    setErrorText(msg)

  private def createBrowserLoginPanel(): JBCefBrowser = {
    val browser = JBCefBrowserBuilder()
      .setUrl("https://www.hackerrank.com/auth/login")
      .build()
//    val requestHandler = CefRemoteRequestHandler(myProject)
//    browser.getJBCefClient.addRequestHandler(requestHandler, browser.getCefBrowser)

    @volatile
    var queueHandle: Option[Queue[IO, Option[HttpCookie]]] = None

    val program =
      for
        queue <- Queue.unbounded[IO, Option[HttpCookie]]
        _     <- IO.delay { queueHandle = Some(queue) }
        _ <- Stream
          .fromQueueNoneTerminated(queue)
          .fold(Nil: List[HttpCookie])((acc, elem) => elem +: acc)
          .evalTap { cookies =>
            cookies.find(_.getName == "remember_hacker_token") match
              case Some(cookie) =>
                // found a candidate cookie, but need to test it to see if it's valid
                implicit val k: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper
                validateUserCookieAndTestLogin[IO](myProject, CodeDojo.HackerRank, cookies).flatMap {
                  case true  => IO.delay(callback(Right(AskForLoginResult.Done)))
                  case false => IO.delay(showLoginError(PluginBundle.message("hackerrank.ui.login.error"))).evalOn(intellijUIContext)
                }
              case None => IO.unit
          }
          .compile
          .drain
      yield ()
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
    browser.getJBCefClient.addLoadHandler(loadHandler, browser.getCefBrowser)
    Disposer.register(
      getDisposable,
      { () =>
//        browser.getJBCefClient.removeRequestHandler(requestHandler, browser.getCefBrowser)
        browser.getJBCefClient.removeLoadHandler(loadHandler, browser.getCefBrowser)
        Disposer.dispose(browser)
      }
    )
    browser
  }
}

object HackerRankLoginDialog {}
