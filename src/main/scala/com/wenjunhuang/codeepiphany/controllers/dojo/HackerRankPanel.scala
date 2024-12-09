package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.IO
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink }
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.wenjunhuang.codeepiphany.controllers.dojo.HackerRankPanel.*
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.groups.*
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.{ LISTS_PROVIDER_KEY, LOGIN_LOGOUT_KEY }
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.{ ListQueryItem, ListsQueryParamProvider, LoginLogoutProvider }
import com.wenjunhuang.codeepiphany.controllers.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.model.{ messages, CodeDojo }
import com.wenjunhuang.codeepiphany.utils.implicits.*

class HackerRankPanel(private val myProject: Project) extends SimpleToolWindowPanel(true, true) with AbstractCodeDojoViewPanel {
  private val actionManager                                   = ActionManager.getInstance()
  private val actionGroup                                     = actionManager.getAction(HACKERRANK_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  private val myActionToolbar                                 = actionManager.createActionToolbar(TOOLBAR_PLACE, actionGroup, true)
  implicit private val httpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper
  private val myApi                                           = HackerRankApi[IO]()
  @volatile
  private var myInitialData = InitialData(UserInfo.empty, Nil)
  @volatile
  private var myState = State(None, Nil, Nil, None, Nil)

  setToolbar(myActionToolbar.getComponent)
  myActionToolbar.setTargetComponent(this)
  setContent(JBLabel("HackerRank"))

  myProject.getMessageBus
    .connect(this)
    .subscribe(
      messages.LOGIN_LOGOUT_TOPIC,
      new messages.LoginLogoutNotifier {
        override def login(codeDojo: CodeDojo): Unit =
          if codeDojo == HackerRank then
            (myApi
              .getInitialData()
              .map { case (userInfo, challengeDomains) =>
                myInitialData = InitialData(userInfo, challengeDomains)
              } *> IO.delay(myActionToolbar.updateActionsAsync()).evalOn(intellijUIContext)).unsafeRunAndForget()

        override def logout(codeDojo: CodeDojo): Unit =
          if codeDojo == HackerRank then
            myInitialData = InitialData(UserInfo.empty, Nil)
            myState = State(None, Nil, Nil, None, Nil)
            IO.delay(myActionToolbar.updateActionsAsync()).evalOn(intellijUIContext).unsafeRunAndForget()
      }
    )

  override def dispose(): Unit = {}

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, makeLoginLogoutProvider())
    dataSink.set(LISTS_PROVIDER_KEY, makeQuestionSheetQueryParamProvider())

  private def makeLoginLogoutProvider(): LoginLogoutProvider = new LoginLogoutProvider {
    override def login(): Unit = askForLogin[IO](myProject, CodeDojo.HackerRank).flatMap {
      case AskForLoginResult.Done =>
        IO.delay {
          myProject.getMessageBus
            .syncPublisher(messages.LOGIN_LOGOUT_TOPIC)
            .login(CodeDojo.HackerRank)
        }
      case _ => IO.unit
    }.unsafeRunAndForget()

    override def logout(): Unit = (askForLogout[IO](myProject, CodeDojo.HackerRank) *> IO.delay {
      myProject.getMessageBus.syncPublisher(messages.LOGIN_LOGOUT_TOPIC).logout(CodeDojo.HackerRank)
    }).unsafeRunAndForget()

    override def isLoggedIn(): Boolean = myInitialData.userInfo != UserInfo.empty
  }

  private def makeQuestionSheetQueryParamProvider(): ListsQueryParamProvider = new ListsQueryParamProvider {
    override def isMultipleSelection(): Boolean = false

    override def getAllItems(): List[ListQueryItem] =
      myInitialData.challengeDomains.map(domain => ListQueryItem(domain.name, domain.slug))

    override def getSelectedItems(): List[ListQueryItem] =
      myState match {
        case State(Some(selectedDomain), _, _, _, _) => ListQueryItem(selectedDomain.name, selectedDomain.slug) :: Nil
        case _                                       => Nil
      }

    override def addSelectedItems(items: List[ListQueryItem]): Unit =
      myState = myState.copy(selectedDomain = myInitialData.challengeDomains.find(_.slug == items.head.id))

    override def removeSelectedItems(items: List[ListQueryItem]): Unit =
      myState = myState match
        case old @ State(currentSelected, subdomains, difficulties, status, skills) =>
          items match
            case first :: _ if currentSelected.contains(first) =>
              State(
                None,
                subdomains,
                difficulties,
                status,
                skills
              )
            case _ => old
  }
}

object HackerRankPanel {
  private case class InitialData(userInfo: UserInfo, challengeDomains: List[ChallengeDomain])
  private case class State(
      selectedDomain: Option[ChallengeDomain],
      selectedSubdomains: List[ChallengeSubdomain],
      selectedDifficulties: List[ChallengeDifficulty],
      selectedStatus: Option[ChallengeStatus],
      selectedSkills: List[ChallengeSkill]
  )
}
