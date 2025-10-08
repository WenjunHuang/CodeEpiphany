package com.wenjunhuang.codeepiphany.codeforces.ui

import cats.effect.IO
import cats.syntax.all.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.{Icon, JComponent}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import com.intellij.ide.util.ChooseElementsDialog
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.scale.JBUIScale

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.LoginAction.{LOGIN_LOGOUT_KEY, LoginLogoutProvider}
import com.wenjunhuang.codeepiphany.actions.UserAccountInfoAction.{USER_ACCOUNT_INFO_KEY, UserInfoProvider}
import com.wenjunhuang.codeepiphany.codeforces.actions.CodeForcesChangeUIAction.{CODEFORCES_CHANGE_UI_PROVIDER_KEY, CodeForcesChangeUIProvider, CodeForcesUI}
import com.wenjunhuang.codeepiphany.codeforces.actions.CodeForcesUpdateProblemSetsAction.{CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY, CodeForcesUpdateProblemSetsProvider}
import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesUserInfo
import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesApi
import com.wenjunhuang.codeepiphany.codeforces.services.problemsets.fetchAndUpdateProblemSets
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettings
import com.wenjunhuang.codeepiphany.database.Tables.CODEFORCES_PROBLEMSETS
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.model.Actions.CODEFORCES_TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.services.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.competitiveCompanion.CCAction.{CC_ACTION_PROVIDER_KEY, CCActionProvider}
import com.wenjunhuang.codeepiphany.utils.competitiveCompanion.startCCListening
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.ui.UnauthenticatedView
import com.wenjunhuang.codeepiphany.utils.AsyncAvatarLoader
import com.wenjunhuang.codeepiphany.vfs.WebPreviewVirtualFile

class CodeForcesChallengesView(private val myProject: Project) extends BaseChallengesView[CodeForcesUI] {

  private val myUnauthenticatedView =
    UnauthenticatedView(CodeForces, Some(PluginBundle.message("needFetchQuestions.tips", CodeForces.show)))

  @volatile
  private var myQueryParamPresenter: Option[CodeForcesParametersQueryPresenter] = None
  @volatile
  private var myKeywordSearchPresenter: Option[CodeForcesKeywordQueryPresenter] = None
  private var myCurrentUI                                                       = CodeForcesUI.Unauthenticated
  private val myLogger                                                          = LoggerFactory.getLogger[IO]

  @volatile
  private var myIsLoggingIn = false

  select(myCurrentUI, false)
  private def initialize(): IO[CodeForcesBootstrapParameters] = {
    CodeForcesApi.getProblemTags.map { tags =>
      CodeForcesBootstrapParameters("*special" +: tags)
    }
  }

  private val myLoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = {
      myIsLoggingIn = true
      (console.info(myProject, CodeForces, PluginBundle.message("console.loggingIn", CodeDojo.CodeForces.show)) *>
        AuthService
          .getInstance(myProject)
          .loadAuthenticationMayAskForLogin(CodeDojo.CodeForces)
          .flatMap {
            case AskForLoginResult.Done =>
              initialize().map { bootstrap =>
                myQueryParamPresenter = Some(CodeForcesParametersQueryPresenter(myProject, bootstrap))
                myKeywordSearchPresenter = Some(CodeForcesKeywordQueryPresenter(myProject, bootstrap))
              } *>
                CodeForcesApi.getUserInfo.flatMap { userInfo =>
                  IO.delay {
                    AuthService.getInstance(myProject).setLogin(CodeDojo.CodeForces, userInfo)
                    val gotoUI = loadLastUI().getOrElse(CodeForcesUI.QueryParameters)
                    mySwitchUIProvider.switchTo(gotoUI)
                  }.evalOnEDTAny()
                }
                *> console.info(
                  myProject,
                  CodeForces,
                  PluginBundle.message("console.loggedIn", CodeDojo.CodeForces.show)
                )
            case _ =>
              console.info(
                myProject,
                CodeForces,
                PluginBundle.message("console.loginCancelled", CodeDojo.CodeForces.show)
              )
          }
          .handleErrorWith { e =>
            myLogger
              .warn(e)("Failed to login") *> console.error(
              myProject,
              PluginBundle.message("console.loginFailed", CodeDojo.CodeForces.show, e.getMessage)
            )
          })
        .guarantee(IO.delay { myIsLoggingIn = false })
        .unsafeRunAsBackgroundProgressCancellable(
          myProject,
          PluginBundle.message("console.loggingIn", CodeDojo.CodeForces.show)
        )
    }

    override def logout(): Unit = (AuthService
      .getInstance(myProject)
      .askForLogout(CodeDojo.CodeForces)
      *> IO.delay {
        AuthService.getInstance(myProject).clearLogin(CodeDojo.CodeForces)
        mySwitchUIProvider.switchTo(CodeForcesUI.Unauthenticated)
      }.evalOnEDTAny()).unsafeRunAndForget()

    override def hasLoggedIn: Boolean = AuthService.getInstance(myProject).isLoggedIn(CodeDojo.CodeForces)

    override def isLoggingIn: Boolean = myIsLoggingIn
  }
  private val myUserInfoProvider = new UserInfoProvider {
    override lazy val avatar: AsyncAvatarLoader =
      AuthService.getInstance(myProject).getLoginUserInfo(CodeDojo.CodeForces) match {
        case Some(userInfo: CodeForcesUserInfo) =>
          AsyncAvatarLoader(userInfo.nickName, userInfo.avatar, JBUIScale.scale(16))
        case _ =>
          AsyncAvatarLoader(PluginBundle.message("user.unknown"), "", JBUIScale.scale(16))
      }

    override lazy val username: String = {
      AuthService.getInstance(myProject).getLoginUserInfo(CodeDojo.CodeForces) match {
        case Some(userInfo: CodeForcesUserInfo) =>
          userInfo.nickName
        case _ => PluginBundle.message("user.unknown")
      }
    }

    override def action: () => Unit = { () =>
      AuthService.getInstance(myProject).getLoginUserInfo(CodeDojo.CodeForces) match {
        case Some(CodeForcesUserInfo(nickName, _)) =>
          HttpClientManager
            .getCookiesForHost(CodeDojo.CodeForces.domain)
            .flatMap { cookies =>
              IO.delay {
                val file = new WebPreviewVirtualFile(
                  s"https://codeforces.com/profile/${nickName}?mobile=true",
                  CodeForces.domain.toString,
                  cookies,
                  CodeForces.show
                )
                WebPreviewVirtualFile.openEditor(file, myProject)
              }.evalOnEDTWithWrite()
            }
            .unsafeRunAndForget()
        case _ =>
      }
    }
  }

  private val mySwitchUIProvider = new CodeForcesChangeUIProvider {
    override def switchTo(ui: CodeForcesUI): Unit =
      myCurrentUI = ui
      select(ui, false)
      saveLastUI(ui)

    override def getCurrentUI: CodeForcesUI = myCurrentUI
  }

  private val myCCProvider = new CCActionProvider {
    @volatile
    private var cancellationToken: Option[() => Future[Unit]] = None

    override def startListening(): Unit = {
      try {
        val v = startCCListening(myProject, CodeDojo.CodeForces, 27121).evalTap { event =>
          event.group.split("-").toList match {
            case group :: _ if CodeDojo.fromCIString(CIString(group.trim)).contains(CodeDojo.CodeForces) =>
              // check if url match CodeForces URL pattern(https://codeforces.com/problemset/problem/954/G)

              val urlPattern = """.*/problemset/problem/(\d+)/([A-Za-z0-9]+)""".r
              event.url match {
                case urlPattern(contestId, index) =>
                  IO.delay {
                    val languages = CodeForcesSettings.getInstance(myProject).getSelectedLanguages
                    val dialog = new ChooseElementsDialog[(Language, LanguageVersion)](
                      myProject,
                      languages.asJava,
                      s"Choose Language",
                      s"Choose Language for ${CodeDojo.CodeForces.show}"
                    ) {
                      override def getItemText(item: (Language, LanguageVersion)): String = {
                        Language.prettyPrint.tupled(item)
                      }
                      override def getItemIcon(item: (Language, LanguageVersion)): Icon = item._1.icon
                    }
                    dialog.showAndGetResult().asScala.headOption.map { selected =>
                      (contestId, index, selected)
                    }
                  }.evalOnEDTAny().flatMap {
                    case Some((contestId, index, selected)) =>
                      ChallengeRepository.getInstance(myProject).getDSLContextResource.use { dsl =>
                        dsl
                          .selectFrom(CODEFORCES_PROBLEMSETS)
                          .where(CODEFORCES_PROBLEMSETS.CONTESTID.eq(contestId.toLong))
                          .and(CODEFORCES_PROBLEMSETS.INDEX.eq(index))
                          .fetchOptional()
                          .toScala match {
                          case Some(existing) =>
                            openChallenge(myProject, selected._1, selected._2, existing)
                          case None =>
                            console.error(
                              myProject,
                              "Challenge not found in database, please update problem sets first."
                            ) *> IO.unit
                        }
                      }
                    case None =>
                      IO.unit
                  }
                case _ => IO.unit
              }
            case _ => IO.unit // Not a CodeForces event, ignore
          }

        }.compile.drain
          .cancelable(IO.unit)
          .unsafeRunCancelable()
          .some
        cancellationToken = v
      } catch {
        case e: Throwable =>
          cancellationToken = None
          (myLogger.error(e)("Failed to start listening for CC events") *>
            console.error(myProject, s"Failed to start listening for CC events because of \"${e.getMessage}\""))
            .unsafeRunAndForget()
      }
    }

    override def stopListening(): Unit = {
      cancellationToken match {
        case Some(cancel) =>
          cancel()
          cancellationToken = None
        case None => // Already not listening
      }
    }

    override def isListening: Boolean = {
      cancellationToken.isDefined
    }
  }

  private val myUpdateProblemsProvider = new CodeForcesUpdateProblemSetsProvider {
    private val myUpdating = AtomicBoolean(false)
    override def updateProblemSets(): Unit =
      if !myUpdating.compareAndExchange(false, true) then
        (fetchAndUpdateProblemSets(myProject).handleErrorWith { e =>
          myLogger.warn(e)("Failed to update CodeForces problem sets") *>
            console.error(myProject, s"Failed to update problem sets because of \"${e.getMessage}\"")
        } *> IO.delay(myUpdating.set(false))).unsafeRunAndForget()

    override def isUpdatingProblemSets: Boolean = myUpdating.get()
  }

  override def getTitleActionGroup: ActionGroup = {
    val actionManager = ActionManager.getInstance()
    val actionGroup   = actionManager.getAction(CODEFORCES_TITLE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
    actionGroup
  }

  override def create(ui: CodeForcesUI): JComponent = ui match {
    case CodeForcesUI.Unauthenticated => myUnauthenticatedView
    case CodeForcesUI.QueryParameters => myQueryParamPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
    case CodeForcesUI.SearchByKeyword =>
      myKeywordSearchPresenter.map(_.getViewComponent).getOrElse(myUnauthenticatedView)
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(USER_ACCOUNT_INFO_KEY, myUserInfoProvider)
    dataSink.set(CODEFORCES_CHANGE_UI_PROVIDER_KEY, mySwitchUIProvider)
    dataSink.set(CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY, myUpdateProblemsProvider)
    dataSink.set(CC_ACTION_PROVIDER_KEY, myCCProvider)

  override def dispose(): Unit = {
    myKeywordSearchPresenter.foreach(Disposer.dispose)
    myQueryParamPresenter.foreach(Disposer.dispose)
  }

  private def saveLastUI(ui: CodeForcesUI): Unit =
    CodeForcesSettings
      .getInstance(myProject)
      .getState
      .queryCriteria
      .put(s"${getClass.getSimpleName}-lastUI", ui.toString)

  private def loadLastUI(): Option[CodeForcesUI] =
    Option(
      CodeForcesSettings
        .getInstance(myProject)
        .getState
        .queryCriteria
        .get(s"${getClass.getSimpleName}-lastUI")
    ).flatMap(value => CodeForcesUI.fromCIStringToAuthenticated(CIString(value)))
}
