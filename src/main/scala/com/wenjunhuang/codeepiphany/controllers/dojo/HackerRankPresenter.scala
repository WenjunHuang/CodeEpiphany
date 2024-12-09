package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.IO
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.{DifficultiesProvider, ListQueryItem, ListsQueryParamProvider, LoginLogoutProvider}
import com.wenjunhuang.codeepiphany.controllers.http.{HttpClientKeeper, HttpClientService}
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{AskForLoginResult, askForLogin, askForLogout}
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.model.{CodeDojo, messages}
import com.wenjunhuang.codeepiphany.utils.implicits.*

import javax.swing.JComponent

class HackerRankPresenter(private val myProject: Project) extends Disposable 
  with LoginLogoutProvider 
  with ListsQueryParamProvider 
  with DifficultiesProvider {
  import HackerRankPresenter.*

  implicit private val httpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(myProject).httpClientKeeper
  private val myApi                                           = HackerRankApi[IO]()
  @volatile
  private var myInitialData = InitialData(UserInfo.empty, Nil)
  @volatile
  private var myState = State(None, Nil, Nil, None, Nil)

  private val myView = HackerRankView(myProject, this)

  Disposer.register(myProject, this)

  myProject.getMessageBus
    .connect(this)
    .subscribe(
      messages.LOGIN_LOGOUT_TOPIC,
      new messages.LoginLogoutNotifier {
        override def login(codeDojo: CodeDojo): Unit =
          if codeDojo == HackerRank then
            myApi
              .getInitialData()
              .map { case (userInfo, challengeDomains) =>
                myInitialData = InitialData(userInfo, challengeDomains)
              }
              .unsafeRunAndForget()
//              *> IO.delay(myActionToolbar.updateActionsAsync()).evalOn(intellijUIContext)).unsafeRunAndForget()

        override def logout(codeDojo: CodeDojo): Unit =
          if codeDojo == HackerRank then
            myInitialData = InitialData(UserInfo.empty, Nil)
            myState = State(None, Nil, Nil, None, Nil)
//            IO.delay(myActionToolbar.updateActionsAsync()).evalOn(intellijUIContext).unsafeRunAndForget()
      }
    )

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

  override def isLoggedIn: Boolean = myInitialData.userInfo != UserInfo.empty

  override def isMultipleSelection: Boolean = false

  override def getAllItems: List[ListQueryItem] =
    myInitialData.challengeDomains.map(domain => ListQueryItem(domain.name, domain.slug))

  override def getSelectedItems: List[ListQueryItem] =
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

  override def getDifficulties: List[actions.Difficulty] = ???

  override def getSelected: List[actions.Difficulty] = ???

  override def addSelected(items: List[actions.Difficulty]): Unit = ???

  override def removeSelected(items: List[actions.Difficulty]): Unit = ???

  override def dispose(): Unit = {}

  def getComponent(): JComponent = myView
}
object HackerRankPresenter {
  private case class InitialData(userInfo: UserInfo, challengeDomains: List[ChallengeDomain])
  private case class State(
      selectedDomain: Option[ChallengeDomain],
      selectedSubdomains: List[ChallengeSubdomain],
      selectedDifficulties: List[ChallengeDifficulty],
      selectedStatus: Option[ChallengeStatus],
      selectedSkills: List[ChallengeSkill]
  )
}
