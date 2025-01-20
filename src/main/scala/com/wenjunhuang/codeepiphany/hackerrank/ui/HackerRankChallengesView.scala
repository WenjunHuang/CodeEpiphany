package com.wenjunhuang.codeepiphany.hackerrank.ui

import cats.effect.IO
import javax.swing.JComponent

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.project.Project
import com.intellij.ui.CardLayoutPanel

import com.wenjunhuang.codeepiphany.actions.LoginAction.{ LOGIN_LOGOUT_KEY, LoginLogoutProvider }
import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.{
  CHANGE_CHALLENGES_UI_PROVIDER_KEY,
  ChallengesUI,
  ChangeChallengesUIProvider
}
import com.wenjunhuang.codeepiphany.model.{ messages, CodeDojo }
import com.wenjunhuang.codeepiphany.model.Actions.HACKERRANK_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.auth.{ askForLogout, loadAuthenticationMayAskForLogin, AskForLoginResult }
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView

class HackerRankChallengesView(private val myProject: Project)
    extends CardLayoutPanel[ChallengesUI, ChallengesUI, JComponent]
    with UiDataProvider
    with Disposable {

  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myUnauthenticatedView    = UnauthenticatedView(HackerRank)
  private val myQueryParamPresenter    = QueryParametersPresenter(myProject)
  private val myKeywordSearchPresenter = KeywordSearchViewPresenter(myProject)
  private var myCurrentUI              = ChallengesUI.Unauthenticated

  @volatile
  private var myHasLoggedIn = false

  @volatile
  private var myIsLoggingIn = false

  select(myCurrentUI, false)

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      myIsLoggingIn = true
      (console.info[IO](myProject, "Logging in to HackerRank...") *>
        loadAuthenticationMayAskForLogin[IO](myProject, CodeDojo.HackerRank).flatMap {
          case AskForLoginResult.Done =>
            myQueryParamPresenter.getInitialData // Load data for the first time before switching to the UI
              *> IO.delay {
                myProject.getMessageBus.syncPublisher(messages.LOGIN_LOGOUT_TOPIC).login(CodeDojo.HackerRank)
                myHasLoggedIn = true
                mySwitchUIProvider.switchTo(ChallengesUI.QueryParameters)
              }.evalOnEDTAny() *> console.info[IO](myProject, "Logged in to HackerRank.")
          case _ => console.info[IO](myProject, "Login to HackerRank canceled.")
        }.handleErrorWith { e => console.error[IO](myProject, s"Login failed because of \"${e.getMessage}\"") })
        .guarantee(IO.delay { myIsLoggingIn = false })
        .unsafeRunAsBackgroundProgressCancellable(myProject, "Logging in to HackerRank...")
    }

    override def logout(): Unit = (askForLogout[IO](myProject, CodeDojo.HackerRank)
      *> IO.delay(myProject.getMessageBus.syncPublisher(messages.LOGIN_LOGOUT_TOPIC).logout(CodeDojo.HackerRank))
      *> IO.delay {
        myHasLoggedIn = false
        mySwitchUIProvider.switchTo(ChallengesUI.Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()

    override def hasLoggedIn: Boolean = myHasLoggedIn

    override def isLoggingIn: Boolean = myIsLoggingIn
  }

  private val mySwitchUIProvider = new ChangeChallengesUIProvider {
    override def switchTo(ui: ChallengesUI): Unit =
      myCurrentUI = ui
      select(ui, false)

    override def getCurrentUI: ChallengesUI = myCurrentUI
  }

  def getActions: ActionGroup = {
    val actionManager = ActionManager.getInstance()
    val actionGroup   = actionManager.getAction(HACKERRANK_TITLE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    actionGroup
  }

  override def prepare(key: ChallengesUI): ChallengesUI = key

  override def create(ui: ChallengesUI): JComponent = ui match {
    case ChallengesUI.Unauthenticated => myUnauthenticatedView
    case ChallengesUI.QueryParameters => myQueryParamPresenter.getComponent
    case ChallengesUI.SearchByKeyword => myKeywordSearchPresenter.getComponent
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(CHANGE_CHALLENGES_UI_PROVIDER_KEY, mySwitchUIProvider)
}
