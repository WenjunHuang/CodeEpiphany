package com.wenjunhuang.codeepiphany.atcoder.ui

import cats.effect.IO
import cats.syntax.all.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.scale.JBUIScale

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.LoginAction.{ LOGIN_LOGOUT_KEY, LoginLogoutProvider }
import com.wenjunhuang.codeepiphany.actions.UserAccountInfoAction.{ USER_ACCOUNT_INFO_KEY, UserInfoProvider }
import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderChangeUIAction.{
  ATCODER_CHANGE_UI_PROVIDER_KEY,
  AtCoderChangeUIProvider,
  AtCoderUI
}
import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderUpdateProblemSetsAction.{
  ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY,
  AtCoderUpdateProblemSetsProvider
}
import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderUserInfo
import com.wenjunhuang.codeepiphany.atcoder.services.AtCoderApi
import com.wenjunhuang.codeepiphany.atcoder.services.problemsets.fetchAndUpdateProblemSets
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettings
import com.wenjunhuang.codeepiphany.model.Actions.ATCODER_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.AtCoder
import com.wenjunhuang.codeepiphany.services.{ console, AskForLoginResult, AuthService, BaseChallengesView }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView
import com.wenjunhuang.codeepiphany.utils.AsyncAvatarLoader
import com.wenjunhuang.codeepiphany.vfs.WebPreviewVirtualFile

class AtCoderChallengesView(private val myProject: Project) extends BaseChallengesView[AtCoderUI] {
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

  private def initialize(): IO[AtCoderBootstrapParameters] =
    AtCoderApi.getUserInfo.map(AtCoderBootstrapParameters.apply)

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      myIsLoggingIn = true
      (console.info(myProject, AtCoder, PluginBundle.message("console.loggingIn", AtCoder.show)) *>
        AuthService
          .getInstance(myProject)
          .loadAuthenticationMayAskForLogin(AtCoder)
          .flatMap {
            case AskForLoginResult.Done =>
              initialize().map { bootstrap =>
                myQueryParamPresenter = AtCoderParametersQueryPresenter(myProject, bootstrap).some
                myKeywordSearchPresenter = AtCoderKeywordQueryPresenter(myProject, bootstrap).some
              } *>
                AtCoderApi.getUserInfo.flatMap { userInfo =>
                  IO.delay {
                    AuthService.getInstance(myProject).setLogin(AtCoder, userInfo)
                    val gotoUI = loadLastUI().getOrElse(AtCoderUI.QueryParameters)
                    mySwitchUIProvider.switchTo(gotoUI)
                  }.evalOnEDTAny() *> console.info(
                    myProject,
                    AtCoder,
                    PluginBundle.message("console.loggedIn", CodeDojo.AtCoder.show)
                  )
                }
            case _ => console.info(myProject, AtCoder, PluginBundle.message("console.loginCancelled", AtCoder.show))
          }
          .handleErrorWith { e =>
            myLogger
              .warn(e)(s"Failed to login ${CodeDojo.AtCoder.show}") *> console.error(
              myProject,
              AtCoder,
              PluginBundle.message("console.loginFailed", AtCoder.show, e.getMessage)
            )
          })
        .guarantee(IO.delay { myIsLoggingIn = false })
        .unsafeRunAsBackgroundProgressCancellable(
          myProject,
          PluginBundle.message("console.loggingIn",AtCoder.show)
        )
    }

    override def logout(): Unit = (AuthService
      .getInstance(myProject)
      .askForLogout(CodeDojo.AtCoder)
      *> IO.delay {
        AuthService.getInstance(myProject).clearLogin(CodeDojo.AtCoder)
        mySwitchUIProvider.switchTo(AtCoderUI.Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()

    override def hasLoggedIn: Boolean = AuthService.getInstance(myProject).isLoggedIn(CodeDojo.AtCoder)

    override def isLoggingIn: Boolean = myIsLoggingIn
  }
  private val myUserInfoProvider = new UserInfoProvider {
    override lazy val avatar: AsyncAvatarLoader =
      AuthService.getInstance(myProject).getLoginUserInfo(CodeDojo.AtCoder) match {
        case Some(userInfo: AtCoderUserInfo) =>
          AsyncAvatarLoader(userInfo.nickName, userInfo.avatar, JBUIScale.scale(16))
        case _ =>
          AsyncAvatarLoader(PluginBundle.message("user.unknown"), "", JBUIScale.scale(16))
      }

    override lazy val username: String = {
      AuthService.getInstance(myProject).getLoginUserInfo(CodeDojo.AtCoder) match {
        case Some(userInfo: AtCoderUserInfo) =>
          userInfo.nickName
        case _ => PluginBundle.message("user.unknown")
      }
    }

    override def action: () => Unit = { () =>
      AuthService.getInstance(myProject).getLoginUserInfo(CodeDojo.AtCoder) match {
        case Some(AtCoderUserInfo(nickName, _)) =>
          HttpClientManager
            .getCookiesForHost(CodeDojo.AtCoder.domain)
            .flatMap { cookies =>
              IO.delay {
                val file = new WebPreviewVirtualFile(
                  s"https://atcoder.jp/users/${nickName}",
                  CodeDojo.AtCoder.domain.toString,
                  cookies,
                  CodeDojo.AtCoder.show
                )
                WebPreviewVirtualFile.openEditor(file, myProject)
              }.evalOnEDTAny()
            }
            .unsafeRunAndForget()
        case _ =>
      }
    }
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
        (fetchAndUpdateProblemSets(myProject).handleErrorWith { e =>
          myLogger.warn(e)("Failed to update AtCoder problem sets") *>
            console.error(myProject, s"Failed to update problem sets because of \"${e.getMessage}\"")
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
    dataSink.set(USER_ACCOUNT_INFO_KEY, myUserInfoProvider)
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
