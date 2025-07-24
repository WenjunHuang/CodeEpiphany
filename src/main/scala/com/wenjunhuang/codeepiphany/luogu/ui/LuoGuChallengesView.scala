package com.wenjunhuang.codeepiphany.luogu.ui

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.wenjunhuang.codeepiphany.actions.LoginAction.{LOGIN_LOGOUT_KEY, LoginLogoutProvider}
import com.wenjunhuang.codeepiphany.luogu.actions.LuoGuChangeUIAction.{LUOGU_CHANGE_UI_PROVIDER_KEY, LuoGuChangeUIProvider, LuoGuUI}
import com.wenjunhuang.codeepiphany.luogu.services.LuoGuApi
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettings
import com.wenjunhuang.codeepiphany.model.Actions.LUOGU_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.LuoGu
import com.wenjunhuang.codeepiphany.services.{AskForLoginResult, AuthService, BaseChallengesView, console}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import javax.swing.JComponent

class LuoGuChallengesView(private val myProject: Project) extends BaseChallengesView[LuoGuUI] {

  private val myUnauthenticatedView =
    UnauthenticatedView(LuoGu, None)

  @volatile
  private var myQueryParamPresenter: Option[LuoGuParametersQueryPresenter] = None
  @volatile
  private var myKeywordSearchPresenter: Option[LuoGuKeywordQueryPresenter] = None
  private var myCurrentUI                                                  = LuoGuUI.Unauthenticated
  private val myLogger                                                     = LoggerFactory.getLogger[IO]

  @volatile
  private var myIsLoggingIn = false

  select(myCurrentUI, false)

  private def initialize(): IO[LuoGuBootstrapParameters] = LuoGuApi.getUserInfo.map(LuoGuBootstrapParameters.apply)

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      myIsLoggingIn = true
      (console.info(myProject, s"Logging in to ${CodeDojo.LuoGu.show}...") *>
        AuthService
          .getInstance(myProject)
          .loadAuthenticationMayAskForLogin(CodeDojo.LuoGu)
          .flatMap {
            case AskForLoginResult.Done =>
              initialize().map { bootstrap =>
                myQueryParamPresenter = Some(LuoGuParametersQueryPresenter(myProject, bootstrap))
                myKeywordSearchPresenter = Some(LuoGuKeywordQueryPresenter(myProject, bootstrap))
              } *> IO.delay {
                AuthService.getInstance(myProject).setLogin(CodeDojo.LuoGu)
                val gotoUI = loadLatestUI().getOrElse(LuoGuUI.QueryParameters)
                mySwitchUIProvider.switchTo(gotoUI)
              }.evalOnEDTAny()
                *> console.info(myProject, s"Logged in to ${CodeDojo.LuoGu.show}.")
            case _ => console.info(myProject, s"Login to ${CodeDojo.LuoGu.show} canceled.")
          }
          .handleErrorWith { e =>
            myLogger.warn(e)("Failed to login") *>
              console.error(myProject, s"Login failed because of \"${e.getMessage}\"")
          })
        .guarantee(IO.delay { myIsLoggingIn = false })
        .unsafeRunAsBackgroundProgressCancellable(myProject, s"Logging in to ${CodeDojo.LuoGu.show}...")
    }

    override def logout(): Unit = (AuthService
      .getInstance(myProject)
      .askForLogout(CodeDojo.LuoGu)
      *> IO.delay {
        AuthService.getInstance(myProject).clearLogin(CodeDojo.LuoGu)
        mySwitchUIProvider.switchTo(LuoGuUI.Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()

    override def hasLoggedIn: Boolean = AuthService.getInstance(myProject).isLoggedIn(CodeDojo.LuoGu)

    override def isLoggingIn: Boolean = myIsLoggingIn
  }

  private val mySwitchUIProvider = new LuoGuChangeUIProvider {
    override def switchTo(ui: LuoGuUI): Unit =
      myCurrentUI = ui
      select(ui, false)
      saveLatestUI(ui)

    override def getCurrentUI: LuoGuUI = myCurrentUI
  }

  override def getTitleActionGroup: ActionGroup = {
    val actionManager = ActionManager.getInstance()
    val actionGroup   = actionManager.getAction(LUOGU_TITLE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    actionGroup
  }

  override def create(ui: LuoGuUI): JComponent = ui match {
    case LuoGuUI.Unauthenticated => myUnauthenticatedView
    case LuoGuUI.QueryParameters => myQueryParamPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
    case LuoGuUI.SearchByKeyword =>
      myKeywordSearchPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(LUOGU_CHANGE_UI_PROVIDER_KEY, mySwitchUIProvider)

  override def dispose(): Unit = {
    myKeywordSearchPresenter.foreach(Disposer.dispose)
    myQueryParamPresenter.foreach(Disposer.dispose)
  }

  private def saveLatestUI(ui: LuoGuUI): Unit = {
    LuoGuSettings.getInstance(myProject).getState.queryCriteria.put(s"${getClass.getSimpleName}-latestUI", ui.toString)
  }

  private def loadLatestUI(): Option[LuoGuUI] =
    val queryCriteria = LuoGuSettings.getInstance(myProject).getState.queryCriteria
    Option(
      queryCriteria
        .get(s"${getClass.getSimpleName}-latestUI")
    ).flatMap(value => LuoGuUI.fromCIStringToAuthenticated(CIString(value)))

}
