package com.wenjunhuang.codeepiphany.hackerrank.ui

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.LoginAction.{LOGIN_LOGOUT_KEY, LoginLogoutProvider}
import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.{CHANGE_CHALLENGES_UI_PROVIDER_KEY, ChangeChallengesUIProvider, HackerRankUI}
import com.wenjunhuang.codeepiphany.hackerrank.models.PROJECT_EULER_DOMAIN
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.model.Actions.HACKERRANK_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.{AskForLoginResult, AuthService, BaseChallengesView, console}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import javax.swing.JComponent

class HackerRankChallengesView(private val myProject: Project) extends BaseChallengesView[HackerRankUI] {

  private val myLogger = LoggerFactory.getLogger[IO]

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
      saveLatestUI(ui)
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
    (console.info(myProject, PluginBundle.message("console.loggingIn", CodeDojo.HackerRank.show)) *>
      AuthService
        .getInstance(myProject)
        .loadAuthenticationMayAskForLogin(CodeDojo.HackerRank)
        .flatMap {
          case AskForLoginResult.Done =>
            initialize().flatMap { initialData =>
              IO.delay {
                myQueryParamPresenter = Some(HackerRankQueryParametersPresenter(myProject, initialData))
                myKeywordSearchPresenter = Some(HackerRankKeywordQueryPresenter(myProject))
                AuthService.getInstance(myProject).setLogin(CodeDojo.HackerRank)

                val gotoUI = loadLatestUI().getOrElse(HackerRankUI.QueryParameters)
                mySwitchUIProvider.switchTo(gotoUI)
              }.evalOnEDTAny() *> console.info(
                myProject,
                PluginBundle.message("console.loggedIn", CodeDojo.HackerRank.show)
              )
            }
          case _ => console.info(myProject, PluginBundle.message("console.loginCancelled", CodeDojo.HackerRank.show))
        }
        .handleErrorWith { e =>
          myLogger.warn(e)("Failed to login") *>
            console.error(
              myProject,
              PluginBundle.message("console.loginFailed", CodeDojo.HackerRank.show, e.getMessage)
            )
        })
      .guarantee(IO.delay { myIsLoggingIn = false })
      .unsafeRunAsBackgroundProgressCancellable(
        myProject,
        PluginBundle.message("console.loggingIn", CodeDojo.HackerRank.show)
      )
  }

  private def performLogout(): Unit = {
    (AuthService.getInstance(myProject).askForLogout(CodeDojo.HackerRank)
      *> IO.delay {
        AuthService.getInstance(myProject).clearLogin(CodeDojo.HackerRank)
        mySwitchUIProvider.switchTo(HackerRankUI.Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()
  }

  private def initialize(): IO[HackerRankBootstrapParameters] = {
    HackerRankApi.getInitialData.map { case (userInfo, challengeDomains) =>
      HackerRankBootstrapParameters(userInfo, challengeDomains.sortBy(_.id) :+ PROJECT_EULER_DOMAIN)
    }
  }

  private def saveLatestUI(ui: HackerRankUI): Unit =
    HackerRankSettings
      .getInstance(myProject)
      .getState
      .queryCriteria
      .put(s"${getClass.getSimpleName}-latestUI", ui.toString)

  private def loadLatestUI(): Option[HackerRankUI] =
    Option(HackerRankSettings.getInstance(myProject).getState.queryCriteria.get(s"${getClass.getSimpleName}-latestUI"))
      .flatMap(value => HackerRankUI.fromCIStringToAuthenticated(CIString(value)))
}
