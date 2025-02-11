package com.wenjunhuang.codeepiphany.hackerrank.ui

import cats.effect.IO
import javax.swing.JComponent
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.project.Project
import com.intellij.ui.CardLayoutPanel

import com.wenjunhuang.codeepiphany.actions.LoginAction.{ LOGIN_LOGOUT_KEY, LoginLogoutProvider }
import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.{
  CHANGE_CHALLENGES_UI_PROVIDER_KEY,
  ChangeChallengesUIProvider,
  HackerRankUI
}
import com.wenjunhuang.codeepiphany.hackerrank.models.PROJECT_EULER_DOMAIN
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.Actions.HACKERRANK_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.{ console, AskForLoginResult, AuthService, BaseChallengesView }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView

class HackerRankChallengesView(private val myProject: Project) extends BaseChallengesView[HackerRankUI] {

  private val myLogger = LoggerFactory.getLogger[IO]
  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myUnauthenticatedView                                             = UnauthenticatedView(HackerRank)
  private var myQueryParamPresenter: Option[HackerRankQueryParametersPresenter] = None
  private var myKeywordSearchPresenter: Option[HackerRankKeywordQueryPresenter] = None
  private var myCurrentUI                                                       = HackerRankUI.Unauthenticated

  @volatile
  private var myIsLoggingIn = false

  select(myCurrentUI, false)

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      performLogin()
    }

    override def logout(): Unit = {
      performLogout()
    }

    override def hasLoggedIn: Boolean = AuthService.getInstance(myProject).isLoggedIn(CodeDojo.HackerRank)

    override def isLoggingIn: Boolean = myIsLoggingIn
  }

  private val mySwitchUIProvider = new ChangeChallengesUIProvider {
    override def switchTo(ui: HackerRankUI): Unit = {
      myCurrentUI = ui
      select(ui, false)
    }

    override def getCurrentUI: HackerRankUI = myCurrentUI
  }

  override def getTitleActionGroup: ActionGroup = {
    val actionManager = ActionManager.getInstance()
    val actionGroup   = actionManager.getAction(HACKERRANK_TITLE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    actionGroup
  }

  override def create(ui: HackerRankUI): JComponent = ui match {
    case HackerRankUI.Unauthenticated => myUnauthenticatedView
    case HackerRankUI.QueryParameters => myQueryParamPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
    case HackerRankUI.SearchByKeyword =>
      myKeywordSearchPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(CHANGE_CHALLENGES_UI_PROVIDER_KEY, mySwitchUIProvider)
  }

  private def performLogin(): Unit = {
    myIsLoggingIn = true
    (console.info[IO](myProject, "Logging in to HackerRank...") *>
      AuthService
        .getInstance(myProject)
        .loadAuthenticationMayAskForLogin[IO](CodeDojo.HackerRank)
        .flatMap {
          case AskForLoginResult.Done =>
            initialize().flatMap { initialData =>
              IO.delay {
                myQueryParamPresenter = Some(HackerRankQueryParametersPresenter(myProject, initialData))
                myKeywordSearchPresenter = Some(HackerRankKeywordQueryPresenter(myProject))
                AuthService.getInstance(myProject).setLogin(CodeDojo.HackerRank)
                mySwitchUIProvider.switchTo(HackerRankUI.QueryParameters)
              }.evalOnEDTAny() *> console.info[IO](myProject, "Logged in to HackerRank.")
            }
          case _ => console.info[IO](myProject, "Login to HackerRank canceled.")
        }
        .handleErrorWith { e =>
          myLogger.warn(e)("Failed to login") *>
            console.error[IO](myProject, s"Login failed because of \"${e.getMessage}\"")
        })
      .guarantee(IO.delay { myIsLoggingIn = false })
      .unsafeRunAsBackgroundProgressCancellable(myProject, "Logging in to HackerRank...")
  }

  private def performLogout(): Unit = {
    (AuthService.getInstance(myProject).askForLogout[IO](CodeDojo.HackerRank)
      *> IO.delay {
        AuthService.getInstance(myProject).clearLogin(CodeDojo.HackerRank)
        mySwitchUIProvider.switchTo(HackerRankUI.Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()
  }

  private def initialize(): IO[HackerRankBootstrapParameters] = {
    HackerRankApi[IO]().getInitialData.map { case (userInfo, challengeDomains) =>
      HackerRankBootstrapParameters(userInfo, challengeDomains.sortBy(_.id) :+ PROJECT_EULER_DOMAIN)
    }
  }
}
