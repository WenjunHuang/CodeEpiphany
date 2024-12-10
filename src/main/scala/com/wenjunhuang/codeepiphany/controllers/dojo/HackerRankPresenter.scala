package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.IO
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.*
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.*
import com.wenjunhuang.codeepiphany.controllers.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeDifficulty.*
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{ askForLogin, askForLogout, AskForLoginResult }
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.model.{ messages, CodeDojo }
import com.wenjunhuang.codeepiphany.utils.Colors
import com.wenjunhuang.codeepiphany.utils.implicits.*

import javax.swing.JComponent

class HackerRankPresenter(private val myProject: Project) extends Disposable {
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

  protected[dojo] def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(LISTS_PROVIDER_KEY, myListsProvider)
    dataSink.set(LOGIN_LOGOUT_KEY, myLoginLogoutProvider)
    dataSink.set(DIFFICULTIES_PROVIDER_KEY, myDifficultiesProvider)
    dataSink.set(STATUS_PROVIDER_KEY, myStatusProvider)
  }

  private val myLoginLogoutProvider = new LoginLogoutProvider {
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
  }

  private val myListsProvider = new ListsQueryParamProvider {
    override def toggleSelection(item: ListQueryItem): Unit =
      if !myState.selectedDomain.exists(_.slug == item.id) && myInitialData.challengeDomains.exists(_.slug == item.id) then
        myState = myState.copy(selectedDomain = myInitialData.challengeDomains.find(_.slug == item.id))

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
  }

  private val myDifficultiesProvider = new DifficultiesProvider {
    override def isSelected(item: Difficulty): Boolean =
      myState.selectedDifficulties.exists(_.value == item.value)

    override def isMultipleSelection: Boolean = true

    override def toggleSelection(item: Difficulty): Unit =
      myState.selectedDifficulties.find(_.value == item.value) match {
        case Some(_) => removeSelected(List(item))
        case None    => addSelected(List(item))
      }

    private def makeHtml(difficulty: ChallengeDifficulty): String =
      difficulty match
        case Easy   => s"<html><font color='${Colors.DIFFICULTY_EASY_COLOR}'>${Easy.show}</font></html>"
        case Medium => s"<html><font color='${Colors.DIFFICULTY_MEDIUM_COLOR}'>${Medium.show}</font></html>"
        case Hard   => s"<html><font color='${Colors.DIFFICULTY_HARD_COLOR}'>${Hard.show}</font></html>"

    override def getDifficulties: List[actions.Difficulty] =
      List(
        actions.Difficulty(makeHtml(ChallengeDifficulty.Easy), ChallengeDifficulty.Easy.value),
        actions.Difficulty(makeHtml(ChallengeDifficulty.Medium), ChallengeDifficulty.Medium.value),
        actions.Difficulty(makeHtml(ChallengeDifficulty.Hard), ChallengeDifficulty.Hard.value)
      )

    override def getSelected: List[actions.Difficulty] = myState.selectedDifficulties.map(difficulty => actions.Difficulty(difficulty.show, difficulty.value))

    override def addSelected(items: List[actions.Difficulty]): Unit =
      myState = myState.copy(selectedDifficulties = (myState.selectedDifficulties ++ items.collect {
        case Difficulty(_, value) if value == ChallengeDifficulty.Easy.value   => ChallengeDifficulty.Easy
        case Difficulty(_, value) if value == ChallengeDifficulty.Medium.value => ChallengeDifficulty.Medium
        case Difficulty(_, value) if value == ChallengeDifficulty.Hard.value   => ChallengeDifficulty.Hard
      }).distinct)

    override def removeSelected(items: List[actions.Difficulty]): Unit =
      myState = myState.copy(selectedDifficulties =
        myState.selectedDifficulties.filterNot(difficulty =>
          items.exists {
            case Difficulty(_, value) if value == difficulty.value => true
            case _                                                 => false
          }
        )
      )
  }

  private val myStatusProvider = new StatusProvider:
    private val allItems = List(ChallengeStatus.Solved, ChallengeStatus.Unsolved)

    override def isSelected(item: Status): Boolean =
      myState.selectedStatus.exists(_.value == item.value)

    override def getAllItems: List[Status] =
      allItems.map(status => Status(status.show, status.value))

    override def isMultipleSelection: Boolean = false

    override def getSelectedItems: List[Status] =
      myState.selectedStatus.map(status => Status(status.show, status.value)).toList

    override def addSelectedItems(items: List[Status]): Unit =
      myState = myState.copy(selectedStatus = items.headOption.flatMap(status => allItems.find(_.value == status.value)))

    override def toggleSelection(item: Status): Unit =
      if myState.selectedStatus.exists(_.value == item.value) then myState = myState.copy(selectedStatus = None)
      else myState = myState.copy(selectedStatus = allItems.find(_.value == item.value))

    override def removeSelectedItems(items: List[Status]): Unit =
      if myState.selectedStatus.exists(_.value == items.head.value) then myState = myState.copy(selectedStatus = None)

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
