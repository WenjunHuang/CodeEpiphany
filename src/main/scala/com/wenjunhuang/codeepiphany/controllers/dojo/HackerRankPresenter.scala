package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.IO
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.DefaultCustomComponentAction
import com.intellij.openapi.actionSystem.{ AnAction, DataSink }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBInsets
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.*
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.*
import com.wenjunhuang.codeepiphany.controllers.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.model.{ ChallengeSkill, * }
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.{ askForLogin, askForLogout, AskForLoginResult }
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.model.{ messages, CodeDojo }
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.Tag as TagUI

import java.awt.{ GridBagConstraints, GridBagLayout }
import javax.swing.{ Icon, JComponent, JPanel }

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
    dataSink.set(SKILL_PROVIDER_KEY, mySkillProvider)
    dataSink.set(TAG_PROVIDER_KEY, myTagProvider)
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
    override def isSelected(item: ListQueryItem): Boolean =
      myState.selectedDomain.exists(_.slug == item.id)

    override def toggleSelection(item: ListQueryItem): Unit =
      if !myState.selectedDomain.exists(_.slug == item.id) && myInitialData.challengeDomains.exists(_.slug == item.id) then
        val newSelected = myInitialData.challengeDomains.find(_.slug == item.id)
        myState = myState.copy(selectedDomain = newSelected, selectedSubdomains = Nil)

        refreshTags()

    override def isMultipleSelection: Boolean = false

    override def getAllItems: List[ListQueryItem] =
      myInitialData.challengeDomains.map(domain => ListQueryItem(domain.name, domain.slug))

    override def getSelectedItems: List[ListQueryItem] =
      myState match {
        case State(Some(selectedDomain), _, _, _, _) => ListQueryItem(selectedDomain.name, selectedDomain.slug) :: Nil
        case _                                       => Nil
      }

    override def addSelectedItems(items: List[ListQueryItem]): Unit =
      myInitialData.challengeDomains.find(_.slug == items.head.id) match
        case Some(newSelected) =>
          if !myState.selectedDomain.exists(_.slug == newSelected.slug) then
            myState = myState.copy(
              selectedDomain = Some(newSelected),
              selectedSubdomains = Nil
            )
            refreshTags()
        case _ =>

    override def removeSelectedItems(items: List[ListQueryItem]): Unit = {}
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

    override def getDifficulties: List[actions.Difficulty] =
      List(
        actions.Difficulty(ChallengeDifficulty.Easy.showAsHtml, ChallengeDifficulty.Easy.value),
        actions.Difficulty(ChallengeDifficulty.Medium.showAsHtml, ChallengeDifficulty.Medium.value),
        actions.Difficulty(ChallengeDifficulty.Hard.showAsHtml, ChallengeDifficulty.Hard.value)
      )

    override def getSelected: List[actions.Difficulty] =
      myState.selectedDifficulties.map(difficulty => actions.Difficulty(difficulty.show, difficulty.value))

    override def addSelected(items: List[actions.Difficulty]): Unit =
      myState = myState.copy(selectedDifficulties = (myState.selectedDifficulties ++ items.collect {
        case Difficulty(_, value) if value == ChallengeDifficulty.Easy.value   => ChallengeDifficulty.Easy
        case Difficulty(_, value) if value == ChallengeDifficulty.Medium.value => ChallengeDifficulty.Medium
        case Difficulty(_, value) if value == ChallengeDifficulty.Hard.value   => ChallengeDifficulty.Hard
      }).distinct)
      refreshTags()

    override def removeSelected(items: List[actions.Difficulty]): Unit =
      myState = myState.copy(selectedDifficulties =
        myState.selectedDifficulties.filterNot(difficulty =>
          items.exists {
            case Difficulty(_, value) if value == difficulty.value => true
            case _                                                 => false
          }
        )
      )
      refreshTags()
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
      refreshTags()

    override def toggleSelection(item: Status): Unit =
      if myState.selectedStatus.exists(_.value == item.value) then myState = myState.copy(selectedStatus = None)
      else myState = myState.copy(selectedStatus = allItems.find(_.value == item.value))
      refreshTags()

    override def removeSelectedItems(items: List[Status]): Unit =
      if myState.selectedStatus.exists(_.value == items.head.value) then
        myState = myState.copy(selectedStatus = None)
        refreshTags()

  private val myTagProvider = new SingleTagGroupProvider {
    override def getAllItems: List[Tag] = myState.selectedDomain.map { domain =>
      domain.subDomains.map(subdomain => Tag(subdomain.name, subdomain.slug, domain.slug))
    }.getOrElse(Nil)

    override def isMultipleSelection: Boolean = true

    override def isSelected(item: Tag): Boolean =
      if myState.selectedDomain.exists(_.slug == item.groupValue) then myState.selectedSubdomains.exists(_.slug == item.value)
      else false

    override def getSelectedItems: List[Tag] = myState.selectedSubdomains.map { subdomain =>
      val domain = myState.selectedDomain.map(_.slug).getOrElse("")
      Tag(subdomain.name, subdomain.slug, domain)
    }

    override def addSelectedItems(items: List[Tag]): Unit =
      myState = myState.copy(
        selectedSubdomains = (myState.selectedSubdomains ++ items.collect {
          case Tag(_, value, groupValue) if myState.selectedDomain.exists(domain => domain.slug == groupValue && domain.subDomains.exists(_.slug == value)) =>
            myState.selectedDomain.flatMap(_.subDomains.find(_.slug == value)).get
        }).distinct
      )
      refreshTags()

    override def removeSelectedItems(items: List[Tag]): Unit =
      myState = myState.copy(selectedSubdomains = myState.selectedSubdomains.filterNot(subdomain => items.exists(_.value == subdomain.slug)))
      refreshTags()

    override def toggleSelection(item: Tag): Unit =
      if isSelected(item) then removeSelectedItems(List(item))
      else addSelectedItems(List(item))
  }

  private val mySkillProvider = new SkillProvider {
    override def getAllItems: List[Skill] = List(
      Skill(
        ChallengeSkill.Basic.show,
        ChallengeSkill.Basic.value
      ),
      Skill(
        ChallengeSkill.Intermediate.show,
        ChallengeSkill.Intermediate.value
      ),
      Skill(
        ChallengeSkill.Advanced.show,
        ChallengeSkill.Advanced.value
      )
    )

    override def isMultipleSelection: Boolean = true

    override def isSelected(item: Skill): Boolean =
      myState.selectedSkills.exists(_.value == item.value)

    override def getSelectedItems: List[Skill] =
      myState.selectedSkills.map(skill => Skill(skill.value, skill.show))

    override def addSelectedItems(items: List[Skill]): Unit =
      myState = myState.copy(selectedSkills = (myState.selectedSkills ++ items.collect {
        case Skill(_, value) if value == ChallengeSkill.Basic.value        => ChallengeSkill.Basic
        case Skill(_, value) if value == ChallengeSkill.Intermediate.value => ChallengeSkill.Intermediate
        case Skill(_, value) if value == ChallengeSkill.Advanced.value     => ChallengeSkill.Advanced
      }).distinct)
      refreshTags()

    override def toggleSelection(item: Skill): Unit =
      if myState.selectedSkills.exists(_.value == item.value) then removeSelectedItems(List(item))
      else addSelectedItems(List(item))

    override def removeSelectedItems(items: List[Skill]): Unit =
      myState = myState.copy(selectedSkills =
        myState.selectedSkills.filterNot(skill =>
          items.exists {
            case Skill(_, value) if value == skill.value => true
            case _                                       => false
          }
        )
      )
      refreshTags()
  }

  private def createTagAction(id: String, text: String, icon: Option[Icon], radius: Float, onCloseAction: Option[() => Unit]): AnAction = DefaultCustomComponentAction { () =>
    val tag = JPanel(GridBagLayout())
    val gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weightx = 1.0
    gbc.weighty = 1.0
    gbc.insets = JBInsets.create(2, 2)
    tag.add(TagUI(id, text, icon, radius, onCloseAction), gbc)
    tag
  }

  private def refreshTags(): Unit = {
    val tagActionGroup = myView.getTagActionGroup
    tagActionGroup.removeAll()

    myState.selectedDomain.foreach { domain =>
      tagActionGroup.add(createTagAction(domain.slug, domain.name, None, DOMAIN_TAG_RADIUS, None))
    }
    myState.selectedStatus.foreach { status =>
      tagActionGroup.add(createTagAction(status.value, status.show, None, STATUS_TAG_RADIUS, Some(() => myStatusProvider.removeSelectedItems(List(Status(status.show, status.value))))))
    }
    myState.selectedDifficulties.foreach { difficulty =>
      tagActionGroup.add(
        createTagAction(
          difficulty.value,
          difficulty.showAsHtml,
          None,
          DIFFICULTY_TAG_RADIUS,
          Some(() => myDifficultiesProvider.removeSelected(List(actions.Difficulty(difficulty.show, difficulty.value))))
        )
      )
    }
    myState.selectedSkills.foreach { skill =>
      tagActionGroup.add(createTagAction(skill.value, skill.show, None, SKILL_TAG_RADIUS, Some(() => mySkillProvider.removeSelectedItems(List(Skill(skill.show, skill.value))))))
    }
    myState.selectedSubdomains.foreach { subdomain =>
      tagActionGroup.add(
        createTagAction(
          subdomain.slug,
          subdomain.name,
          None,
          SUBDOMAIN_TAG_RADIUS,
          Some(() => myTagProvider.removeSelectedItems(List(Tag(subdomain.name, subdomain.slug, myState.selectedDomain.map(_.slug).getOrElse("")))))
        )
      )
    }

    myView.refreshTagToolbar()
  }

  override def dispose(): Unit = {}

  def getComponent: JComponent = myView
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

  private val DOMAIN_TAG_RADIUS     = 0.2f
  private val DIFFICULTY_TAG_RADIUS = 0.4f
  private val STATUS_TAG_RADIUS     = 0.5f
  private val SKILL_TAG_RADIUS      = 0.6f
  private val SUBDOMAIN_TAG_RADIUS  = 1.0f
}
