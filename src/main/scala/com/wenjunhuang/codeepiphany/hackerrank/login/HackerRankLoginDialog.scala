package com.wenjunhuang.codeepiphany.hackerrank.login

import cats.effect.IO
import cats.effect.std.Queue
import com.intellij.ide.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{ComponentValidator, DialogWrapper, ValidationInfo}
import com.intellij.openapi.util.Disposer
import com.intellij.ui.{AnimatedIcon, DocumentAdapter, PopupHandler}
import com.intellij.ui.components.{JBScrollPane, JBTextArea}
import com.intellij.ui.content.{ContentManagerEvent, ContentManagerListener, TabbedPaneContentUI}
import com.intellij.ui.content.impl.ContentManagerImpl
import com.intellij.ui.jcef.{JBCefBrowser, JBCefBrowserBuilder}
import com.intellij.util.ui.JBUI
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{validateUserCookieAndTestLogin, AskForLoginResult}
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.http.{HttpClientKeeper, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.isDebug
import fs2.Stream
import org.cef.browser.CefBrowser
import org.cef.callback.CefCookieVisitor
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefCookie
import org.typelevel.log4cats.LoggerFactory

import java.awt.Font
import java.awt.datatransfer.{DataFlavor, StringSelection}
import java.awt.event.ActionEvent
import java.net.HttpCookie
import javax.swing.*
import javax.swing.event.DocumentEvent
import scala.jdk.CollectionConverters.*

class HackerRankLoginDialog(private val myProject: Project, private val callback: Either[Throwable, AskForLoginResult] => Unit)
    extends DialogWrapper(myProject, false, DialogWrapper.IdeModalityType.IDE) {
  private val myLogger         = LoggerFactory[IO].getLogger
  private val myContentManager = ContentManagerImpl(TabbedPaneContentUI(SwingConstants.TOP), false, myProject, getDisposable)

  private val myCookieText = JBTextArea(10, 20)
  myCookieText.setLineWrap(true)
  myCookieText.setFont(JBUI.Fonts.label())

  private val fontOptions = AppEditorFontOptions.getInstance().getState
  myCookieText.setFont(Font(fontOptions.FONT_FAMILY, Font.PLAIN, fontOptions.FONT_SIZE))
  ComponentValidator(myDisposable)
    .installOn(myCookieText)

  myCookieText.getDocument.addDocumentListener(new DocumentAdapter {
    override def textChanged(e: DocumentEvent): Unit = {
      ComponentValidator
        .getInstance(myCookieText)
        .ifPresent(v => v.updateInfo(null))
      e.getDocument.getLength match
        case 0 => setOKActionEnabled(false)
        case _ => setOKActionEnabled(true)
    }
  })

  private val myCookieLoginPane = createCookieLoginPane()
  private val myLoginViaCookiePanel = myContentManager.getFactory.createContent(
    myCookieLoginPane,
    PluginBundle.message("hackerrank.ui.login.viaCookie"),
    true
  )

  if !isDebug then
    PopupHandler.installPopupMenu(
      myCookieText,
      IdeActions.GROUP_CUT_COPY_PASTE,
      ActionPlaces.POPUP
    )
    
  private val myLoginBrowser = createBrowserLoginPanel()
  private val myLoginViaBrowserPanel = myContentManager.getFactory.createContent(
    JBScrollPane(myLoginBrowser.getComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
    PluginBundle.message("hackerrank.ui.login.viaBrowser"),
    true
  )
  implicit private val myHttpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper

  private val myOkAction: OkAction = new OkAction {
    override def doAction(e: ActionEvent): Unit = {
      val text = myCookieText.getText
      myCookieText.setEnabled(false)
      myOkAction.setEnabled(false)
      myOkAction.putValue(Action.SMALL_ICON, AnimatedIcon.Default.INSTANCE)
      myOkAction.putValue(Action.NAME, PluginBundle.message("hackerrank.ui.login.validating"))
      validateUserCookieAndTestLogin[IO](myProject, HackerRank, text).flatMap {
        case true => IO.delay(callback(Right(AskForLoginResult.Done))) *> IO.delay(close(DialogWrapper.OK_EXIT_CODE, true)).evalOn(intellijUIContext)
        case false =>
          IO.delay {
            ComponentValidator
              .getInstance(myCookieText)
              .ifPresent(v => v.updateInfo(ValidationInfo(PluginBundle.message("hackerrank.ui.login.cookie.error"), myCookieText)))

            myCookieText.setEnabled(true)
            myCookieText.requestFocus()
            myOkAction.setEnabled(true)
            myOkAction.putValue(Action.SMALL_ICON, null)
            myOkAction.putValue(Action.NAME, PluginBundle.message("hackerrank.ui.login.ok"))
          }.evalOn(intellijUIContext)
      }.unsafeRunAndForget()
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

  private def createCookieLoginPane(): JComponent =
    new JBScrollPane(myCookieText, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) with UiDataProvider {
      override def uiDataSnapshot(dataSink: DataSink): Unit = {
        dataSink.set(
          PlatformDataKeys.CUT_PROVIDER,
          new CutProvider {
            override def performCut(dataContext: DataContext): Unit = {
              val text      = myCookieText.getSelectedText
              val selection = new StringSelection(text)
              CopyPasteManager.getInstance().setContents(selection)
              myCookieText.getDocument.remove(myCookieText.getSelectionStart, myCookieText.getSelectionEnd - myCookieText.getSelectionStart)
            }

            override def isCutEnabled(dataContext: DataContext): Boolean =
              myCookieText.getSelectedText != null

            override def isCutVisible(dataContext: DataContext): Boolean = true
          }
        )
        dataSink.set(
          PlatformDataKeys.COPY_PROVIDER,
          new CopyProvider {
            override def performCopy(dataContext: DataContext): Unit =
              CopyPasteManager.getInstance().setContents(new StringSelection(myCookieText.getSelectedText))

            override def isCopyEnabled(dataContext: DataContext): Boolean =
              myCookieText.getSelectedText != null

            override def isCopyVisible(dataContext: DataContext): Boolean = true
          }
        )
        dataSink.set(
          PlatformDataKeys.PASTE_PROVIDER,
          new PasteProvider {
            override def performPaste(dataContext: DataContext): Unit = {
              val copyPasteManager = CopyPasteManager.getInstance()
              val content          = copyPasteManager.getContents[String](DataFlavor.stringFlavor)
              myCookieText.getDocument.insertString(myCookieText.getCaretPosition, content, null)
            }

            override def isPastePossible(dataContext: DataContext): Boolean = true

            override def isPasteEnabled(dataContext: DataContext): Boolean = true
          }
        )
      }
    }
  private def createBrowserLoginPanel(): JBCefBrowser = {
    val browser = JBCefBrowserBuilder()
      .setUrl("https://www.hackerrank.com/auth/login")
      .build()

    enum CookieCheck {
      case Add(cookie: HttpCookie)
      case Check
    }

    @volatile
    var queueHandle: Option[Queue[IO, Option[CookieCheck]]] = None

    val cookieProcessingStream =
      for
        queue <- Queue.unbounded[IO, Option[CookieCheck]]
        _     <- IO.delay { queueHandle = Some(queue) }
        _ <- Stream
          .fromQueueNoneTerminated(queue)
          .mapAccumulate(Nil: List[HttpCookie]) {
            case (acc, CookieCheck.Add(cookie)) =>
              (cookie +: acc, None)
            case (acc, CookieCheck.Check) =>
              (Nil, Some(acc))
          }
          .collect { case (_, Some(cookies)) => cookies }
          .evalTap { cookies =>
            cookies.find(_.getName == "remember_hacker_token") match
              case Some(_) =>
                // found a candidate cookie, but need to test it to see if it's valid
                validateUserCookieAndTestLogin[IO](myProject, CodeDojo.HackerRank, cookies).flatMap {
                  case true =>
                    IO.delay(callback(Right(AskForLoginResult.Done))) *> IO.delay {
                      close(DialogWrapper.OK_EXIT_CODE)
                    }.evalOn(intellijUIContext)
                  case false => myLogger.warn("Browser login failed")
                }
              case None => IO.unit
          }
          .onFinalize(myLogger.info("Cookie processing stream finalized"))
          .compile
          .drain
      yield ()
    cookieProcessingStream.unsafeRunAndForget()

    val loadHandler = new CefLoadHandlerAdapter {
      override def onLoadingStateChange(cefBrowser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean): Unit =
        browser.getJBCefCookieManager.getCefCookieManager.visitAllCookies { (cefCookie: CefCookie, count: Int, total: Int, _: BoolRef) =>
          if cefCookie.domain.contains("hackerrank.com") then
            val cookie = new HttpCookie(cefCookie.name, cefCookie.value)
            cookie.setDomain(cefCookie.domain)
            cookie.setPath(cefCookie.path)

            if count == total - 1 then queueHandle.foreach(q => (q.offer(Some(CookieCheck.Add(cookie))) *> q.offer(Some(CookieCheck.Check))).unsafeRunAndForget())
            else queueHandle.foreach(_.offer(Some(CookieCheck.Add(cookie))).unsafeRunAndForget())
          else if count == total - 1 then queueHandle.foreach(q => q.offer(None).unsafeRunAndForget())
          true
        }
    }
    browser.getJBCefClient.addLoadHandler(loadHandler, browser.getCefBrowser)

    Disposer.register(
      getDisposable,
      { () =>
        queueHandle.foreach(_.offer(None).unsafeRunAndForget())
        browser.getJBCefCookieManager.getCefCookieManager.deleteCookies(null, null)
        browser.getJBCefClient.removeLoadHandler(loadHandler, browser.getCefBrowser)
        Disposer.dispose(browser)
      }
    )
    browser
  }
}

object HackerRankLoginDialog {}
