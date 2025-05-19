package com.wenjunhuang.codeepiphany.services.login

import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import java.awt.{BorderLayout, Font}
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
import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{ComponentValidator, DialogPanel, DialogWrapper, ValidationInfo}
import com.intellij.openapi.util.Disposer
import com.intellij.ui.{AnimatedIcon, DocumentAdapter, PopupHandler}
import com.intellij.ui.components.{JBScrollPane, JBTextArea}
import com.intellij.ui.jcef.{JBCefBrowser, JBCefBrowserBuilder}
import com.intellij.ui.tabs.{JBTabsEx, JBTabsFactory, TabInfo, TabsListener}
import com.intellij.util.ui.JBUI

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.{AskForLoginResult, AuthService}
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.actions.{DataSink, UiDataProvider}
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.isDebug

class LoginDialog(
  private val myProject: Project,
  private val myCodeDojo: CodeDojo,
  private val myLoginCallback: Either[Throwable, AskForLoginResult] => Unit
) extends DialogWrapper(myProject, false, DialogWrapper.IdeModalityType.MODELESS) {
  private val myLogger = LoggerFactory[IO].getLogger
  private val myTabs   = JBTabsFactory.createTabs(myProject, myDisposable).asInstanceOf[JBTabsEx]

  private val myCookieText = JBTextArea(10, 20)
  myCookieText.setLineWrap(true)
  myCookieText.setBorder(JBUI.Borders.compound(JBUI.Borders.empty(5), DarculaTextBorder()))
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
  myTabs.addTab(
    new TabInfo(myCookieLoginPane)
      .setText(PluginBundle.message("loginDialog.viaCookie"))
      .setObject(myCookieLoginPane)
  )

  private val myLoginBrowser = createBrowser()
  myTabs.addTab(
    new TabInfo(
      JBScrollPane(
        myLoginBrowser.getComponent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      )
    ).setText(PluginBundle.message("loginDialog.viaBrowser"))
      .setObject(myLoginBrowser)
  )

  myTabs.addListener(
    new TabsListener {
      override def selectionChanged(oldSelection: TabInfo, newSelection: TabInfo): Unit = {
        if newSelection.getObject == myCookieLoginPane then getButton(myOkAction).setVisible(true)
        else if newSelection.getObject == myLoginBrowser then
          myLoginBrowser.getComponent.requestFocus()
          getButton(myOkAction).setVisible(false)
      }
    },
    myDisposable
  )

  private implicit val myHttpClientKeeper: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myOkAction: DialogWrapper#OkAction = new OkAction {
    override def doAction(e: ActionEvent): Unit = {
      val text = myCookieText.getText
      myCookieText.setEnabled(false)
      myOkAction.setEnabled(false)
      myOkAction.putValue(Action.SMALL_ICON, AnimatedIcon.Default.INSTANCE)
      myOkAction.putValue(Action.NAME, PluginBundle.message("loginDialog.validating"))

      AuthService
        .getInstance(myProject)
        .validateUserCookieAndTestLogin[IO](myCodeDojo, text)
        .flatMap {
          case true =>
            IO.delay(close(DialogWrapper.OK_EXIT_CODE, true))
              .evalOnEDTAny() *>
              IO.delay(myLoginCallback(Right(AskForLoginResult.Done)))
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
        }
        .unsafeRunAndForget()
    }
  }

  private val myDialogPane = DialogPanel(BorderLayout())
  myDialogPane.add(myTabs.getComponent, BorderLayout.CENTER)
  myDialogPane.setPreferredFocusedComponent(myCookieText)

  init()
  setTitle(PluginBundle.message("loginDialog.title", myCodeDojo.show))
  setOKButtonText(PluginBundle.message("loginDialog.ok"))

  if !isDebug then PopupHandler.installPopupMenu(myCookieText, IdeActions.GROUP_CUT_COPY_PASTE, ActionPlaces.POPUP)

  override def createCenterPanel(): JComponent = myDialogPane

  override def doCancelAction(): Unit =
    myLoginCallback(Right(AskForLoginResult.Cancelled))
    super.doCancelAction()

  override def getOKAction: Action = myOkAction

  override def createActions(): Array[Action] = {
    val helpAction: Action = new AbstractAction(PluginBundle.message("loginDialog.help")) {
      override def actionPerformed(e: ActionEvent): Unit =
        BrowserUtil.browse(s"https://github.com/WenjunHuang/CodeEpiphany/wiki/Log-in#${myCodeDojo.value}")
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
    var myQueueHandle: Option[Queue[IO, CookieCheck]] = None

    val myCookieProcessingStreamCanceller =
      (for
        queue <- Queue.unbounded[IO, CookieCheck]
        _     <- IO.delay { myQueueHandle = Some(queue) }
        _ <- Stream
          .fromQueueUnterminated(queue)
          .evalTap { cc =>
            myLogger.info(s"Cookie processing stream received ${cc}")
          }
          .mapAccumulate(Nil: List[HttpCookie]) {
            case (acc, CookieCheck.Add(cookie)) =>
              (cookie +: acc, None)
            case (acc, CookieCheck.Check) =>
              (Nil, Some(acc))
          }
          .collect { case (_, Some(cookies)) => cookies }
          .evalTap { cookies =>
            IO.delay {
              myCodeDojo.loginCandidateCookies(cookies)
            }.flatMap { result =>
              if result then
                // found a candidate cookie, but need to test it to see if it's valid
                myLogger.info(s"Found login cookies for $myCodeDojo") *>
                  AuthService
                    .getInstance(myProject)
                    .validateUserCookieAndTestLogin[IO](myCodeDojo, cookies)
                    .flatMap {
                      case true =>
                        myLogger.info(s"Browser login $myCodeDojo successful") *>
                          IO.delay(myLoginCallback(Right(AskForLoginResult.Done))) *>
                          IO.delay(close(DialogWrapper.OK_EXIT_CODE)).evalOnEDTAny()

                      case false =>
                        myLogger.warn("Browser login failed")
                    }
              else myLogger.info(s"Browser login failed due to no candidate cookie. CodeDojo:$myCodeDojo")
            }
          }
          .onFinalizeCase { existCase => myLogger.info(s"Cookie processing stream finalized because of ${existCase}") }
          .compile
          .drain
      yield ()).unsafeRunCancelable()

    val loadHandler = new CefLoadHandlerAdapter {
      override def onLoadingStateChange(
        cefBrowser: CefBrowser,
        isLoading: Boolean,
        canGoBack: Boolean,
        canGoForward: Boolean
      ): Unit = {
        browser.getJBCefCookieManager.getCefCookieManager.visitAllCookies {
          (cefCookie: CefCookie, count: Int, total: Int, _: BoolRef) =>
            if CIString(cefCookie.domain).contains(myCodeDojo.domain) then
              val cookie = new HttpCookie(cefCookie.name, cefCookie.value)
              cookie.setDomain(cefCookie.domain)
              cookie.setPath(cefCookie.path)

              if count == total - 1 then
                myQueueHandle.foreach(q =>
                  (q.offer(CookieCheck.Add(cookie)) *> q.offer(CookieCheck.Check)).unsafeRunAndForget()
                )
              else myQueueHandle.foreach(_.offer(CookieCheck.Add(cookie)).unsafeRunAndForget())
            true
        }
      }
    }
    browser.getJBCefClient.addLoadHandler(loadHandler, browser.getCefBrowser)

    Disposer.register(
      getDisposable,
      { () =>
        myCookieProcessingStreamCanceller()
        browser.getJBCefCookieManager.getCefCookieManager.deleteCookies(null, null)
        browser.getJBCefClient.removeLoadHandler(loadHandler, browser.getCefBrowser)
        Disposer.dispose(browser)
      }
    )
    browser
  }
}
