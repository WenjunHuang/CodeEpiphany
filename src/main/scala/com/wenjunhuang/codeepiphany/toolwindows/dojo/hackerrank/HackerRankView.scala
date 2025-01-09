package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import cats.effect.IO
import com.intellij.openapi.actionSystem.{ DataSink, UiDataProvider }
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.ui.CardLayoutPanel
import com.wenjunhuang.codeepiphany.actions.LoginAction.{ LOGIN_LOGOUT_KEY, LoginLogoutProvider }
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{
  askForLogout,
  loadAuthenticationMayAskForLogin,
  AskForLoginResult
}
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.{ messages, CodeDojo }
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.actions.SwitchUIAction.{
  SWITCHUI_PROVIDER_KEY,
  SwitchUIProvider
}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

import javax.swing.JComponent

class HackerRankView(private val myProject: Project)
    extends CardLayoutPanel[HackerRankUI, HackerRankUI, JComponent]
    with UiDataProvider
    with Disposable {

  implicit private val httpClientKeeper: HttpClientKeeper[IO] =
    HttpClientService.getInstance(myProject).httpClientKeeper

  private val myUnauthenticatedView    = UnauthenticatedView()
  private val myQueryParamPresenter    = QueryParametersPresenter(myProject)
  private val myKeywordSearchPresenter = KeywordSearchViewPresenter(myProject)
  private var myCurrentUI              = HackerRankUI.Unauthenticated

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
                mySwitchUIProvider.switchTo(HackerRankUI.QueryParameters)
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
        select(HackerRankUI.Unauthenticated, false)
      }.evalOnEDTAny()).unsafeRunAndForget()

    override def hasLoggedIn: Boolean = myHasLoggedIn

    override def isLoggingIn: Boolean = myIsLoggingIn
  }

  private val mySwitchUIProvider = new SwitchUIProvider {
    override def switchTo(ui: HackerRankUI): Unit =
      myCurrentUI = ui
      select(ui, false)

    override def getCurrentUI: HackerRankUI = myCurrentUI
  }

  override def prepare(key: HackerRankUI): HackerRankUI = key

  override def create(ui: HackerRankUI): JComponent = ui match {
    case HackerRankUI.Unauthenticated => myUnauthenticatedView
    case HackerRankUI.QueryParameters => myQueryParamPresenter.getComponent
    case HackerRankUI.SearchByKeyword => myKeywordSearchPresenter.getComponent
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(SWITCHUI_PROVIDER_KEY, mySwitchUIProvider)
}
