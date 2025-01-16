package com.wenjunhuang.codeepiphany.services.login

import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import java.awt.Font
import java.awt.datatransfer.{DataFlavor, StringSelection}
import java.awt.event.ActionEvent
import java.net.HttpCookie
import javax.swing.*
import javax.swing.event.DocumentEvent
import org.cef.browser.CefBrowser
import org.cef.callback.CefCookieVisitor
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefCookie
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

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
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.auth.{validateUserCookieAndTestLogin, AskForLoginResult}
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.isDebug

class LoginDialog(
  private val myProject: Project,
  private val myCodeDojo: CodeDojo,
  private val myLoginCallback: Either[Throwable, AskForLoginResult] => Unit
) extends DialogWrapper(myProject, false, DialogWrapper.IdeModalityType.IDE) {
  private val myLogger = LoggerFactory[IO].getLogger
  private val myContentManager =
    ContentManagerImpl(TabbedPaneContentUI(SwingConstants.TOP), false, myProject, getDisposable)

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
  private val myLoginViaCookiePanel =
    myContentManager.getFactory.createContent(myCookieLoginPane, PluginBundle.message("loginDialog.viaCookie"), true)

  if !isDebug then PopupHandler.installPopupMenu(myCookieText, IdeActions.GROUP_CUT_COPY_PASTE, ActionPlaces.POPUP)

  private val myLoginBrowser = createBrowser()
  private val myLoginViaBrowserPanel = myContentManager.getFactory.createContent(
    JBScrollPane(
      myLoginBrowser.getComponent,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ),
    PluginBundle.message("loginDialog.viaBrowser"),
    true
  )

  implicit private val myHttpClientKeeper: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myOkAction: OkAction = new OkAction {
    override def doAction(e: ActionEvent): Unit = {
      val text = myCookieText.getText
      myCookieText.setEnabled(false)
      myOkAction.setEnabled(false)
      myOkAction.putValue(Action.SMALL_ICON, AnimatedIcon.Default.INSTANCE)
      myOkAction.putValue(Action.NAME, PluginBundle.message("loginDialog.validating"))
      validateUserCookieAndTestLogin[IO](myProject, myCodeDojo, text).flatMap {
        case true =>
          IO.delay(myLoginCallback(Right(AskForLoginResult.Done))) *> IO
            .delay(close(DialogWrapper.OK_EXIT_CODE, true))
            .evalOnEDTAny()
        case false =>
          IO.delay {
            ComponentValidator
              .getInstance(myCookieText)
              .ifPresent(v =>
                v.updateInfo(ValidationInfo(PluginBundle.message("loginDialog.cookie.error"), myCookieText))
              )

            myCookieText.setEnabled(true)
            myCookieText.requestFocus()
            myOkAction.setEnabled(true)
            myOkAction.putValue(Action.SMALL_ICON, null)
            myOkAction.putValue(Action.NAME, PluginBundle.message("loginDialog.ok"))
          }.evalOnEDTAny()
      }.unsafeRunAndForget()
    }
  }

  myContentManager.addContent(myLoginViaCookiePanel)
  myContentManager.addContent(myLoginViaBrowserPanel)
  myContentManager.addContentManagerListener(new ContentManagerListener {
    override def selectionChanged(event: ContentManagerEvent): Unit =
      event.getOperation match
        case ContentManagerEvent.ContentOperation.add =>
          if event.getContent == myLoginViaCookiePanel then getButton(myOkAction).setVisible(true)
          else if event.getContent == myLoginViaBrowserPanel then
            myLoginBrowser.getComponent.requestFocus()
            getButton(myOkAction).setVisible(false)
        case _ =>
  })

  init()
  setTitle(PluginBundle.message("loginDialog.title", myCodeDojo.show))
  setOKButtonText(PluginBundle.message("loginDialog.ok"))

  override def createCenterPanel(): JComponent = myContentManager.getComponent

  override def doCancelAction(): Unit =
    myLoginCallback(Right(AskForLoginResult.Cancelled))
    super.doCancelAction()

  override def getOKAction: Action = myOkAction

  override def createActions(): Array[Action] = {
    val helpAction: Action = new AbstractAction(PluginBundle.message("loginDialog.help")) {
      override def actionPerformed(e: ActionEvent): Unit = ???
    }
    Array(helpAction, myOkAction, getCancelAction)
  }

  private def createCookieLoginPane(): JComponent =
    new JBScrollPane(
      myCookieText,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ) with UiDataProvider {
      override def uiDataSnapshot(dataSink: DataSink): Unit = {
        dataSink.set(
          PlatformDataKeys.CUT_PROVIDER,
          new CutProvider {
            override def performCut(dataContext: DataContext): Unit = {
              val text      = myCookieText.getSelectedText
              val selection = new StringSelection(text)
              CopyPasteManager.getInstance().setContents(selection)
              myCookieText.getDocument
                .remove(myCookieText.getSelectionStart, myCookieText.getSelectionEnd - myCookieText.getSelectionStart)
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
  private def createBrowser(): JBCefBrowser = {
    val builder = JBCefBrowserBuilder()
    if isDebug then
      builder
        .setOffScreenRendering(false)
        .setEnableOpenDevToolsMenuItem(true)
    else
      builder
        .setOffScreenRendering(true)

    val browser = builder
      .setUrl(myCodeDojo.getLoginURL)
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
            if myCodeDojo.loginCandidateCookies(cookies) then
              // found a candidate cookie, but need to test it to see if it's valid
              validateUserCookieAndTestLogin[IO](myProject, myCodeDojo, cookies).flatMap {
                case true =>
                  IO.delay(myLoginCallback(Right(AskForLoginResult.Done))) *>
                    IO.delay(close(DialogWrapper.OK_EXIT_CODE)).evalOnEDTAny()
                case false => myLogger.warn("Browser login failed")
              }
            else myLogger.warn(s"Browser login failed due to no candidate cookie. CodeDojo:$myCodeDojo")
          }
          .onFinalize(myLogger.info("Cookie processing stream finalized"))
          .compile
          .drain
      yield ()
    cookieProcessingStream.unsafeRunAndForget()

    val loadHandler = new CefLoadHandlerAdapter {
      override def onLoadingStateChange(
        cefBrowser: CefBrowser,
        isLoading: Boolean,
        canGoBack: Boolean,
        canGoForward: Boolean
      ): Unit =
        browser.getJBCefCookieManager.getCefCookieManager.visitAllCookies {
          (cefCookie: CefCookie, count: Int, total: Int, _: BoolRef) =>
            if CIString(cefCookie.domain).contains(myCodeDojo.domain) then
              val cookie = new HttpCookie(cefCookie.name, cefCookie.value)
              cookie.setDomain(cefCookie.domain)
              cookie.setPath(cefCookie.path)

              if count == total - 1 then
                queueHandle.foreach(q =>
                  (q.offer(Some(CookieCheck.Add(cookie))) *> q.offer(Some(CookieCheck.Check))).unsafeRunAndForget()
                )
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
