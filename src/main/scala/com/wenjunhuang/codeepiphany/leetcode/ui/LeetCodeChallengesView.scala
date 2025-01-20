package com.wenjunhuang.codeepiphany.leetcode.ui

import cats.effect.IO
import cats.syntax.all.*
import javax.swing.JComponent
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.project.Project
import com.intellij.ui.CardLayoutPanel

import com.wenjunhuang.codeepiphany.actions.LoginAction.{ LOGIN_LOGOUT_KEY, LoginLogoutProvider }
import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeChangeUIAction.{
  LEETCODE_CHANGE_UI_PROVIDER_KEY,
  LeetCodeChangeUIProvider,
  LeetCodeUI
}
import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeChangeUIAction.LeetCodeUI.*
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.Actions.LEETCODE_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.services.{ console, AskForLoginResult, AuthService }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView

class LeetCodeChallengesView(private val myProject: Project, private val myCodeDojo: CodeDojo)
    extends CardLayoutPanel[LeetCodeUI, LeetCodeUI, JComponent]
    with UiDataProvider
    with Disposable {

  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myUnauthenticatedView    = UnauthenticatedView(myCodeDojo)
  private val myQueryParamPresenter    = QueryParametersPresenter(myProject, myCodeDojo)
  private val myKeywordSearchPresenter = KeywordSearchViewPresenter(myProject, myCodeDojo)
  private var myCurrentUI              = LeetCodeUI.Unauthenticated
  private val myLogger                 = LoggerFactory.getLogger[IO]

  @volatile
  private var myIsLoggingIn = false

  select(myCurrentUI, false)

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      myIsLoggingIn = true
      (console.info[IO](myProject, s"Logging in to ${myCodeDojo.show}...") *>
        AuthService
          .getInstance(myProject)
          .loadAuthenticationMayAskForLogin[IO](myCodeDojo)
          .flatMap {
            case AskForLoginResult.Done =>
              myQueryParamPresenter.initialize().map { initData =>
                myKeywordSearchPresenter.setInitialData(
                  initData
                ) // Load data for the first time before switching to the UI
              } *> IO.delay {
                AuthService.getInstance(myProject).setLogin(myCodeDojo)
                mySwitchUIProvider.switchTo(QueryParameters)
              }.evalOnEDTAny()
                *> console.info[IO](myProject, s"Logged in to ${myCodeDojo.show}.")
            case _ => console.info[IO](myProject, s"Login to ${myCodeDojo.show} canceled.")
          }
          .handleErrorWith { e =>
            myLogger.warn(e)("Failed to login") *>
              console.error[IO](myProject, s"Login failed because of \"${e.getMessage}\"")
          })
        .guarantee(IO.delay { myIsLoggingIn = false })
        .unsafeRunAsBackgroundProgressCancellable(myProject, s"Logging in to ${myCodeDojo.show}...")
    }

    override def logout(): Unit = (AuthService
      .getInstance(myProject)
      .askForLogout[IO](myCodeDojo)
      *> IO.delay {
        AuthService.getInstance(myProject).clearLogin(myCodeDojo)
        mySwitchUIProvider.switchTo(Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()

    override def hasLoggedIn: Boolean = AuthService.getInstance(myProject).isLoggedIn(myCodeDojo)

    override def isLoggingIn: Boolean = myIsLoggingIn
  }

  private val mySwitchUIProvider = new LeetCodeChangeUIProvider {
    override def switchTo(ui: LeetCodeUI): Unit =
      myCurrentUI = ui
      select(ui, false)

    override def getCurrentUI: LeetCodeUI = myCurrentUI
  }

  def getActions: ActionGroup = {
    val actionManager = ActionManager.getInstance()
    val actionGroup   = actionManager.getAction(LEETCODE_TITLE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    actionGroup
  }

  override def prepare(key: LeetCodeUI): LeetCodeUI = key

  override def create(ui: LeetCodeUI): JComponent = ui match {
    case LeetCodeUI.Unauthenticated => myUnauthenticatedView
    case LeetCodeUI.QueryParameters => myQueryParamPresenter.getComponent
    case LeetCodeUI.SearchByKeyword => myKeywordSearchPresenter.getComponent
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(LEETCODE_CHANGE_UI_PROVIDER_KEY, mySwitchUIProvider)
}
