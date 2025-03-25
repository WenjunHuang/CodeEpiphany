package com.wenjunhuang.codeepiphany.atcoder.ui

import cats.effect.IO
import cats.syntax.all.*
import icons.CodeEpiphanyIcons
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

import com.wenjunhuang.codeepiphany.actions.LoginAction.{ LOGIN_LOGOUT_KEY, LoginLogoutProvider }
import com.wenjunhuang.codeepiphany.model.Actions.ATCODER_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.AtCoder
import com.wenjunhuang.codeepiphany.services.{ console, AskForLoginResult, AuthService, BaseChallengesView }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderChangeUIAction.{
  ATCODER_CHANGE_UI_PROVIDER_KEY,
  AtCoderChangeUIProvider,
  AtCoderUI
}
import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderUpdateProblemSetsAction.{
  ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY,
  AtCoderUpdateProblemSetsProvider
}
import com.wenjunhuang.codeepiphany.atcoder.services.AtCoderApi
import com.wenjunhuang.codeepiphany.atcoder.services.problemsets.fetchAndUpdateProblemSets
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettings
import com.wenjunhuang.codeepiphany.utils.actions.DataSink

class AtCoderChallengesView(private val myProject: Project) extends BaseChallengesView[AtCoderUI] {

  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myUnauthenticatedView =
    UnauthenticatedView(AtCoder, Some(PluginBundle.message("needFetchQuestions.tips", AtCoder.show)))

  @volatile
  private var myQueryParamPresenter: Option[AtCoderParametersQueryPresenter] = None
  @volatile
  private var myKeywordSearchPresenter: Option[AtCoderKeywordQueryPresenter] = None
  private var myCurrentUI                                                    = AtCoderUI.Unauthenticated
  private val myLogger                                                       = LoggerFactory.getLogger[IO]

  @volatile
  private var myIsLoggingIn = false

  select(myCurrentUI, false)
  private def initialize(): IO[AtCoderBootstrapParameters] = {
    AtCoderApi[IO]().getUserInfo.map { userInfo =>
      AtCoderBootstrapParameters(userInfo)
    }
  }

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      myIsLoggingIn = true
      (console.info[IO](myProject, s"Logging in to ${CodeDojo.AtCoder.show}...") *>
        AuthService
          .getInstance(myProject)
          .loadAuthenticationMayAskForLogin[IO](CodeDojo.AtCoder)
          .flatMap {
            case AskForLoginResult.Done =>
              initialize().map { bootstrap =>
                myQueryParamPresenter = Some(AtCoderParametersQueryPresenter(myProject, bootstrap))
                myKeywordSearchPresenter = Some(AtCoderKeywordQueryPresenter(myProject, bootstrap))
              } *> IO.delay {
                AuthService.getInstance(myProject).setLogin(CodeDojo.AtCoder)
                val gotoUI = loadLastUI().getOrElse(AtCoderUI.QueryParameters)
                mySwitchUIProvider.switchTo(gotoUI)
              }.evalOnEDTAny()
                *> console.info[IO](myProject, s"Logged in to ${CodeDojo.AtCoder.show}.")
            case _ => console.info[IO](myProject, s"Login to ${CodeDojo.AtCoder.show} canceled.")
          }
          .handleErrorWith { e =>
            myLogger.warn(e)("Failed to login") *>
              console.error[IO](myProject, s"Login failed because of \"${e.getMessage}\"")
          })
        .guarantee(IO.delay { myIsLoggingIn = false })
        .unsafeRunAsBackgroundProgressCancellable(myProject, s"Logging in to ${CodeDojo.AtCoder.show}...")
    }

    override def logout(): Unit = (AuthService
      .getInstance(myProject)
      .askForLogout[IO](CodeDojo.AtCoder)
      *> IO.delay {
        AuthService.getInstance(myProject).clearLogin(CodeDojo.AtCoder)
        mySwitchUIProvider.switchTo(AtCoderUI.Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()

    override def hasLoggedIn: Boolean = AuthService.getInstance(myProject).isLoggedIn(CodeDojo.AtCoder)

    override def isLoggingIn: Boolean = myIsLoggingIn
  }

  private val mySwitchUIProvider = new AtCoderChangeUIProvider {
    override def switchTo(ui: AtCoderUI): Unit =
      myCurrentUI = ui
      select(ui, false)
      saveLastUI(ui)

    override def getCurrentUI: AtCoderUI = myCurrentUI
  }

  private val myUpdateProblemsProvider = new AtCoderUpdateProblemSetsProvider {
    private val myUpdating = AtomicBoolean(false)
    override def updateProblemSets(): Unit =
      if !myUpdating.compareAndExchange(false, true) then
        (fetchAndUpdateProblemSets[IO](myProject).handleErrorWith { e =>
          myLogger.warn(e)("Failed to update AtCoder problem sets") *>
            console.error[IO](myProject, s"Failed to update problem sets because of \"${e.getMessage}\"")
        } *> IO.delay(myUpdating.set(false))).unsafeRunAndForget()

    override def isUpdatingProblemSets: Boolean = myUpdating.get()
  }

  override def getTitleActionGroup: ActionGroup = {
    val actionManager = ActionManager.getInstance()
    val actionGroup   = actionManager.getAction(ATCODER_TITLE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    actionGroup
  }

  override def create(ui: AtCoderUI): JComponent = ui match {
    case AtCoderUI.Unauthenticated => myUnauthenticatedView
    case AtCoderUI.QueryParameters => myQueryParamPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
    case AtCoderUI.SearchByKeyword =>
      myKeywordSearchPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(ATCODER_CHANGE_UI_PROVIDER_KEY, mySwitchUIProvider)
    dataSink.set(ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY, myUpdateProblemsProvider)

  override def dispose(): Unit = {
    myKeywordSearchPresenter.foreach(Disposer.dispose)
    myQueryParamPresenter.foreach(Disposer.dispose)
  }

  private def saveLastUI(ui: AtCoderUI): Unit = {
    val queryCriteriaStorage = AtCoderSettings.getInstance(myProject).getState.queryCriteria
    queryCriteriaStorage.put(s"${getClass.getSimpleName}-lastUI", ui.toString)
  }

  private def loadLastUI(): Option[AtCoderUI] = {
    val queryCriteriaStorage = AtCoderSettings.getInstance(myProject).getState.queryCriteria
    Option(queryCriteriaStorage.get(s"${getClass.getSimpleName}-lastUI")).flatMap(value =>
      AtCoderUI.fromCIStringToAuthenticated(CIString(value))
    )
  }
}
