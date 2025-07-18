package com.wenjunhuang.codeepiphany.leetcode.ui

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.LoginAction.{LOGIN_LOGOUT_KEY, LoginLogoutProvider}
import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeChangeUIAction.LeetCodeUI.*
import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeChangeUIAction.{LEETCODE_CHANGE_UI_PROVIDER_KEY, LeetCodeChangeUIProvider, LeetCodeUI}
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.leetcode.settings.{LeetCodeCNSettings, LeetCodeSettings}
import com.wenjunhuang.codeepiphany.model.Actions.LEETCODE_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.{AskForLoginResult, AuthService, BaseChallengesView, console}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import javax.swing.JComponent

class LeetCodeChallengesView(
  private val myProject: Project,
  private val myCodeDojo: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
) extends BaseChallengesView[LeetCodeUI] {

  private def initialize(): IO[LeetCodeBootstrapParameters] = {
    val myApi = LeetCodeApi(myCodeDojo)
    (
      myApi.getUserInfo.retryLimitsWithBackoff(),
      myApi.getCategoryList.retryLimitsWithBackoff(),
      myApi.getFavoriteList.retryLimitsWithBackoff(),
      myApi.getTagTypeWithTags.retryLimitsWithBackoff(),
      myApi.getQuestionCompanyTags
        .map(it => it.sortBy(_.questionCount)(using Ordering.Int.reverse))
        .retryLimitsWithBackoff(),
      myApi.getPositionTags.retryLimitsWithBackoff()
    ).parMapN { (userInfo, categories, favorites, tagTypeWithTags, companyTags, positionTags) =>
      LeetCodeBootstrapParameters(userInfo, categories, favorites, tagTypeWithTags, companyTags, positionTags)
    }
  }

  private val myUnauthenticatedView = UnauthenticatedView(myCodeDojo)
  @volatile
  private var myQueryParamPresenter: Option[LeetCodeParametersQueryPresenter] = None
  @volatile
  private var myKeywordSearchPresenter: Option[LeetCodeKeywordQueryPresenter] = None
  @volatile
  private var myCompanyQueryPresenter: Option[LeetCodeCompanyQueryPresenter] = None
  private var myCurrentUI                                                    = LeetCodeUI.Unauthenticated
  private val myLogger                                                       = LoggerFactory.getLogger[IO]

  @volatile
  private var myIsLoggingIn = false

  select(myCurrentUI, false)

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      myIsLoggingIn = true
      (console.info(myProject, PluginBundle.message("console.loggingIn", myCodeDojo.show)) *>
        AuthService
          .getInstance(myProject)
          .loadAuthenticationMayAskForLogin(myCodeDojo)
          .flatMap {
            case AskForLoginResult.Done =>
              initialize().map { bootstrap =>
                myQueryParamPresenter = Some(LeetCodeParametersQueryPresenter(myProject, bootstrap, myCodeDojo))
                myKeywordSearchPresenter = Some(LeetCodeKeywordQueryPresenter(myProject, bootstrap, myCodeDojo))
                myCompanyQueryPresenter = Some(LeetCodeCompanyQueryPresenter(myProject, bootstrap, myCodeDojo))
              } *> IO.delay {
                AuthService.getInstance(myProject).setLogin(myCodeDojo)
                val gotoUI = loadLastUI().getOrElse(QueryParameters)
                mySwitchUIProvider.switchTo(gotoUI)
              }.evalOnEDTAny()
                *> console.info(myProject, PluginBundle.message("console.loggedIn", myCodeDojo.show))
            case _ => console.info(myProject, PluginBundle.message("console.loginCancelled", myCodeDojo.show))
          }
          .handleErrorWith { e =>
            myLogger.warn(e)("Failed to login") *>
              console.error(myProject, PluginBundle.message("console.loginFailed", myCodeDojo.show, e.getMessage))
          })
        .guarantee(IO.delay { myIsLoggingIn = false })
        .unsafeRunAsBackgroundProgressCancellable(myProject, PluginBundle.message("console.loggingIn", myCodeDojo.show))
    }

    override def logout(): Unit = (AuthService
      .getInstance(myProject)
      .askForLogout(myCodeDojo)
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
      saveLastUI(ui)

    override def getCurrentUI: LeetCodeUI = myCurrentUI
  }

  override def getTitleActionGroup: ActionGroup = {
    val actionManager = ActionManager.getInstance()
    val actionGroup   = actionManager.getAction(LEETCODE_TITLE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    actionGroup
  }

  override def prepare(key: LeetCodeUI): LeetCodeUI = key

  override def create(ui: LeetCodeUI): JComponent = ui match {
    case LeetCodeUI.Unauthenticated => myUnauthenticatedView
    case LeetCodeUI.QueryParameters => myQueryParamPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
    case LeetCodeUI.SearchByKeyword => myKeywordSearchPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
    case LeetCodeUI.CompanyQuery    => myCompanyQueryPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(LEETCODE_CHANGE_UI_PROVIDER_KEY, mySwitchUIProvider)

  private def saveLastUI(ui: LeetCodeUI): Unit = {
    val queryCriteriaStorage = myCodeDojo match
      case CodeDojo.LeetCodeCN => LeetCodeCNSettings.getInstance(myProject).getState.queryCriteria
      case CodeDojo.LeetCode   => LeetCodeSettings.getInstance(myProject).getState.queryCriteria
    queryCriteriaStorage.put(s"${getClass.getSimpleName}-lastUI", ui.toString)
  }

  private def loadLastUI(): Option[LeetCodeUI] = {
    val queryCriteriaStorage = myCodeDojo match
      case CodeDojo.LeetCodeCN => LeetCodeCNSettings.getInstance(myProject).getState.queryCriteria
      case CodeDojo.LeetCode   => LeetCodeSettings.getInstance(myProject).getState.queryCriteria
    Option(queryCriteriaStorage.get(s"${getClass.getSimpleName}-lastUI")).flatMap(value =>
      LeetCodeUI.fromCIStringToAuthenticated(CIString(value))
    )
  }
}
