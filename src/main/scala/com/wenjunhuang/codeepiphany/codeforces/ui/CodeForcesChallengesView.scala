package com.wenjunhuang.codeepiphany.codeforces.ui

import cats.effect.IO
import cats.syntax.all.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.project.Project
import com.intellij.ui.CardLayoutPanel

import com.wenjunhuang.codeepiphany.actions.LoginAction.{ LOGIN_LOGOUT_KEY, LoginLogoutProvider }
import com.wenjunhuang.codeepiphany.codeforces.actions.CodeForcesChangeUIAction.{
  CODEFORCES_CHANGE_UI_PROVIDER_KEY,
  CodeForcesChangeUIProvider,
  CodeForcesUI
}
import com.wenjunhuang.codeepiphany.codeforces.actions.CodeForcesUpdateProblemSetsAction.{
  CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY,
  CodeForcesUpdateProblemSetsProvider
}
import com.wenjunhuang.codeepiphany.codeforces.services.problemsets.fetchAndUpdateProblemSets
import com.wenjunhuang.codeepiphany.model.Actions.{ CODEFORCES_TITLE_TOOLBAR_GROUP, LEETCODE_TITLE_TOOLBAR_GROUP }
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.services.{ console, AskForLoginResult, AuthService }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView

class CodeForcesChallengesView(private val myProject: Project)
    extends CardLayoutPanel[CodeForcesUI, CodeForcesUI, JComponent]
    with UiDataProvider
    with Disposable {

  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myUnauthenticatedView    = UnauthenticatedView(CodeForces)
  private val myQueryParamPresenter    = QueryParametersPresenter(myProject)
  private val myKeywordSearchPresenter = KeywordSearchViewPresenter(myProject)
  private var myCurrentUI              = CodeForcesUI.Unauthenticated
  private val myLogger                 = LoggerFactory.getLogger[IO]

  @volatile
  private var myIsLoggingIn = false

  select(myCurrentUI, false)

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      myIsLoggingIn = true
      (console.info[IO](myProject, s"Logging in to ${CodeDojo.CodeForces.show}...") *>
        AuthService
          .getInstance(myProject)
          .loadAuthenticationMayAskForLogin[IO](CodeDojo.CodeForces)
          .flatMap {
            case AskForLoginResult.Done =>
              myQueryParamPresenter.initialize().map { initData =>
                myKeywordSearchPresenter.setInitialData(
                  initData
                ) // Load data for the first time before switching to the UI
              } *> IO.delay {
                AuthService.getInstance(myProject).setLogin(CodeDojo.CodeForces)
                mySwitchUIProvider.switchTo(CodeForcesUI.QueryParameters)
              }.evalOnEDTAny()
                *> console.info[IO](myProject, s"Logged in to ${CodeDojo.CodeForces.show}.")
            case _ => console.info[IO](myProject, s"Login to ${CodeDojo.CodeForces.show} canceled.")
          }
          .handleErrorWith { e =>
            myLogger.warn(e)("Failed to login") *>
              console.error[IO](myProject, s"Login failed because of \"${e.getMessage}\"")
          })
        .guarantee(IO.delay { myIsLoggingIn = false })
        .unsafeRunAsBackgroundProgressCancellable(myProject, s"Logging in to ${CodeDojo.CodeForces.show}...")
    }

    override def logout(): Unit = (AuthService
      .getInstance(myProject)
      .askForLogout[IO](CodeDojo.CodeForces)
      *> IO.delay {
        AuthService.getInstance(myProject).clearLogin(CodeDojo.CodeForces)
        mySwitchUIProvider.switchTo(CodeForcesUI.Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()

    override def hasLoggedIn: Boolean = AuthService.getInstance(myProject).isLoggedIn(CodeDojo.CodeForces)

    override def isLoggingIn: Boolean = myIsLoggingIn
  }

  private val mySwitchUIProvider = new CodeForcesChangeUIProvider {
    override def switchTo(ui: CodeForcesUI): Unit =
      myCurrentUI = ui
      select(ui, false)

    override def getCurrentUI: CodeForcesUI = myCurrentUI
  }

  private val myUpdateProblemsProvider = new CodeForcesUpdateProblemSetsProvider {
    private val myUpdating = AtomicBoolean(false)
    override def updateProblemSets(): Unit =
      if !myUpdating.compareAndExchange(false, true) then
        (fetchAndUpdateProblemSets[IO](myProject).handleErrorWith { e =>
          myLogger.warn(e)("Failed to update CodeForces problem sets") *>
            console.error[IO](myProject, s"Failed to update problem sets because of \"${e.getMessage}\"")
        } *> IO.delay(myUpdating.set(false))).unsafeRunAndForget()

    override def isUpdatingProblemSets: Boolean = myUpdating.get()
  }

  def getActions: ActionGroup = {
    val actionManager = ActionManager.getInstance()
    val actionGroup   = actionManager.getAction(CODEFORCES_TITLE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    actionGroup
  }

  override def prepare(key: CodeForcesUI): CodeForcesUI = key

  override def create(ui: CodeForcesUI): JComponent = ui match {
    case CodeForcesUI.Unauthenticated => myUnauthenticatedView
    case CodeForcesUI.QueryParameters => myQueryParamPresenter.getComponent
    case CodeForcesUI.SearchByKeyword => myKeywordSearchPresenter.getComponent
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(CODEFORCES_CHANGE_UI_PROVIDER_KEY, mySwitchUIProvider)
    dataSink.set(CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY, myUpdateProblemsProvider)
}
