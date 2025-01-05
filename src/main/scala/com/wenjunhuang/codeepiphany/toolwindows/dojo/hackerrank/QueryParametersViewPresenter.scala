package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import cats.effect.IO
import cats.effect.std.Queue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ AnAction, DataSink }
import com.intellij.openapi.actionSystem.ex.DefaultCustomComponentAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBInsets
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.hackerrank.services.editor.openChallenge
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.{ messages, CodeDojo, Language }
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.*
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.*
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers.ChallengeProvider
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.ui.Tag as TagUI
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.typelevel.ci.CIString
import org.typelevel.log4cats.{ Logger, LoggerFactory }

import java.awt.{ GridBagConstraints, GridBagLayout }
import javax.swing.{ Icon, JComponent, JPanel }
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class QueryParametersViewPresenter(private val myProject: Project) extends Disposable {
  import QueryParametersViewPresenter.*

  implicit private val myLogger: Logger[IO] = LoggerFactory[IO].getLogger

  implicit private val httpClientKeeper: HttpClientKeeper[IO] =
    HttpClientService.getInstance(myProject).httpClientKeeper
  private val myApi = HackerRankApi[IO]()

  private val myView = QueryParamView(myProject, this)

  @volatile
  private var myInitialData = InitialData(EMPTY_USERINFO, Nil)
  @volatile
  private var myState = State(PROJECT_EULER_DOMAIN, Nil, Nil, None, Nil)

  @volatile
  private var myQueryQueue: Option[Queue[IO, Option[State]]] = None

  private val myQueryWorker = for {
    q              <- Queue.unbounded[IO, Option[State]]
    _              <- IO.delay { myQueryQueue = Some(q) }
    notInterrupted <- SignallingRef.of[IO, Boolean](false)
    _ <- Stream
      .fromQueueNoneTerminated(q)
      .evalMapAccumulate(notInterrupted) { case (signal, state) =>
        for {
          _         <- signal.set(true)
          newSignal <- SignallingRef.of[IO, Boolean](false)
        } yield (newSignal, state)
      }
      .debounce(200.millis)
      .evalTap { case (signal, state) =>
        val from  = math.max((state.currentPage - 1) * state.pageSize.value, 0)
        val limit = state.pageSize.value
        Stream
          .eval(
            myApi
              .searchChallenges(
                from,
                limit,
                state.selectedDomain.contest,
                state.selectedDomain.slug,
                state.selectedStatus.toList,
                state.selectedSkills,
                state.selectedDifficulties,
                state.selectedSubdomains
              )
              .map { case (totalSize, items) => state.copy(currentItems = items, totalSize = totalSize) }
              .flatMap { state =>
                IO.delay {
                  myState = state
                  myPaginationProvider.refresh()
                  updateChallengeItems(state.currentItems)
                }.evalOnEDTAny()
              }
          )
          .interruptWhen(signal)
          .attempt
          .evalMap {
            case Left(e) =>
              myLogger.warn(e)("Error while querying challenges")
            case _ => IO.unit
          }
          .compile
          .drain
      }
      .onFinalize(myLogger.info("Query worker is finalized"))
      .compile
      .drain
  } yield ()

  myQueryWorker.unsafeRunAndForget()

  myProject.getMessageBus
    .connect(this)
    .subscribe(
      messages.LOGIN_LOGOUT_TOPIC,
      new messages.LoginLogoutNotifier {
        override def login(codeDojo: CodeDojo): Unit =
          if codeDojo == HackerRank then
            myApi.getInitialData.map { case (userInfo, challengeDomains) =>
              myInitialData = InitialData(userInfo, challengeDomains.sortBy(_.id) :+ PROJECT_EULER_DOMAIN)
              myState = EMPTY_STATE.copy(selectedDomain = myInitialData.challengeDomains.head)
              refreshTags()
              refreshQuery()
            }.unsafeRunAndForget()

        override def logout(codeDojo: CodeDojo): Unit =
          if codeDojo == HackerRank then
            myInitialData = InitialData(EMPTY_USERINFO, Nil)
            myState = State(PROJECT_EULER_DOMAIN, Nil, Nil, None, Nil)
      }
    )

  Disposer.register(myProject, this)

  protected[dojo] def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(LISTS_PROVIDER_KEY, myListsProvider)
    dataSink.set(DIFFICULTIES_PROVIDER_KEY, myDifficultiesProvider)
    dataSink.set(STATUS_PROVIDER_KEY, myStatusProvider)
    dataSink.set(SKILL_PROVIDER_KEY, mySkillProvider)
    dataSink.set(TAG_PROVIDER_KEY, myTagProvider)
    dataSink.set(PAGINATION_PROVIDER_KEY, myPaginationProvider)
    dataSink.set(CHALLENGE_PROVIDER_KEY, myChallengeProvider)
  }

  private def refreshQuery(resetToFirstPage: Boolean = true): Unit =
    myQueryQueue.foreach { q =>
      val state = if resetToFirstPage then myState.resetToFirstPage() else myState
      q.offer(Some(state)).unsafeRunAndForget()
    }
  private val myChallengeProvider = new ChallengeProvider {
    override def openCurrentSelectedChallenge(language: Language): Unit = {
      Option(myView.getTable.getSelectedObject) match
        case Some(selected) =>
          openChallenge[IO](
            myProject,
            selected.slug,
            Contest.fromCIString(CIString(selected.contestSlug)).get,
            language
          ).unsafeRunAsBackgroundProgress(myProject, "Opening challenge")
        case None => ()
    }

    override def getLanguages: List[Language] = List(Language.Java)
  }

  private val myListsProvider = new CategoryProvider {
    override def isSelected(item: Category): Boolean =
      myState.selectedDomain.slug == item.value

    override def toggleSelection(item: Category): Unit =
      if myState.selectedDomain.slug != item.value && myInitialData.challengeDomains.exists(_.slug == item.value) then
        myInitialData.challengeDomains.find(_.slug == item.value).foreach { newSelected =>
          myState = myState.copy(selectedDomain = newSelected, selectedSubdomains = Nil).resetPagination()
          refreshTags()
          refreshQuery()
        }

    override def isMultipleSelection: Boolean = false

    override def getAllItems: List[Category] =
      myInitialData.challengeDomains.map(domain => Category(domain.name, domain.slug))

    override def getSelectedItems: List[Category] =
      List(Category(myState.selectedDomain.name, myState.selectedDomain.slug))

    override def addSelectedItems(items: List[Category]): Unit =
      myInitialData.challengeDomains.find(_.slug == items.head.value) match
        case Some(newSelected) =>
          if myState.selectedDomain.slug != newSelected.slug then
            myState = myState.copy(selectedDomain = newSelected, selectedSubdomains = Nil)
            refreshTags()
        case _ =>

    override def removeSelectedItems(items: List[Category]): Unit = {}
  }

  private val myDifficultiesProvider = new DifficultiesProvider {
    override def isSelected(item: Difficulty): Boolean =
      myState.selectedDifficulties.exists(_.value == item.value)

    override def isMultipleSelection: Boolean = true

    override def toggleSelection(item: Difficulty): Unit =
      myState.selectedDifficulties.find(_.value == item.value) match {
        case Some(_) => removeSelectedItems(List(item))
        case None    => addSelectedItems(List(item))
      }

    override def getAllItems: List[Difficulty] =
      List(
        Difficulty(ChallengeDifficulty.Easy.showAsHtml, ChallengeDifficulty.Easy.value),
        Difficulty(ChallengeDifficulty.Medium.showAsHtml, ChallengeDifficulty.Medium.value),
        Difficulty(ChallengeDifficulty.Hard.showAsHtml, ChallengeDifficulty.Hard.value)
      )

    override def getSelectedItems: List[Difficulty] =
      myState.selectedDifficulties.map(difficulty => Difficulty(difficulty.show, difficulty.value))

    override def addSelectedItems(items: List[Difficulty]): Unit =
      myState = myState.copy(selectedDifficulties = (myState.selectedDifficulties ++ items.collect {
        case Difficulty(_, value) if value == ChallengeDifficulty.Easy.value   => ChallengeDifficulty.Easy
        case Difficulty(_, value) if value == ChallengeDifficulty.Medium.value => ChallengeDifficulty.Medium
        case Difficulty(_, value) if value == ChallengeDifficulty.Hard.value   => ChallengeDifficulty.Hard
      }).distinct)
      refreshTags()
      refreshQuery()

    override def removeSelectedItems(items: List[Difficulty]): Unit =
      myState = myState.copy(selectedDifficulties =
        myState.selectedDifficulties.filterNot(difficulty =>
          items.exists {
            case Difficulty(_, value) if value == difficulty.value => true
            case _                                                 => false
          }
        )
      )
      refreshTags()
      refreshQuery()
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
      myState =
        myState.copy(selectedStatus = items.headOption.flatMap(status => allItems.find(_.value == status.value)))
      refreshTags()
      refreshQuery()

    override def toggleSelection(item: Status): Unit =
      if myState.selectedStatus.exists(_.value == item.value) then myState = myState.copy(selectedStatus = None)
      else myState = myState.copy(selectedStatus = allItems.find(_.value == item.value))
      refreshTags()
      refreshQuery()

    override def removeSelectedItems(items: List[Status]): Unit =
      if myState.selectedStatus.exists(_.value == items.head.value) then
        myState = myState.copy(selectedStatus = None)
        refreshTags()
        refreshQuery()

  private val myTagProvider = new SingleTagGroupProvider {
    override def getAllItems: List[Tag] =
      val domain = myState.selectedDomain
      domain.subDomains.map(subdomain => Tag(subdomain.name, subdomain.slug, domain.slug))

    override def isMultipleSelection: Boolean = true

    override def isSelected(item: Tag): Boolean =
      if myState.selectedDomain.slug == item.groupValue then myState.selectedSubdomains.exists(_.slug == item.value)
      else false

    override def getSelectedItems: List[Tag] = myState.selectedSubdomains.map { subdomain =>
      val domain = myState.selectedDomain.slug
      Tag(subdomain.name, subdomain.slug, domain)
    }

    override def addSelectedItems(items: List[Tag]): Unit =
      myState = myState.copy(selectedSubdomains = (myState.selectedSubdomains ++ items.collect {
        case Tag(_, value, groupValue)
            if myState.selectedDomain.slug == groupValue && myState.selectedDomain.subDomains.exists(_.slug == value) =>
          myState.selectedDomain.subDomains.find(_.slug == value).get
      }).distinct)
      refreshTags()
      refreshQuery()

    override def removeSelectedItems(items: List[Tag]): Unit =
      myState = myState.copy(selectedSubdomains =
        myState.selectedSubdomains.filterNot(subdomain => items.exists(_.value == subdomain.slug))
      )
      refreshTags()
      refreshQuery()

    override def toggleSelection(item: Tag): Unit =
      if isSelected(item) then removeSelectedItems(List(item))
      else addSelectedItems(List(item))
  }

  private val mySkillProvider = new SkillProvider {
    override def getAllItems: List[Skill] = List(
      Skill(ChallengeSkill.Basic.show, ChallengeSkill.Basic.value),
      Skill(ChallengeSkill.Intermediate.show, ChallengeSkill.Intermediate.value),
      Skill(ChallengeSkill.Advanced.show, ChallengeSkill.Advanced.value)
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
      refreshQuery()

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
      refreshQuery()

  }

  private val myPaginationProvider = new PaginationProvider {
    private val allItems = List(PageSize.Twenty, PageSize.Fifty, PageSize.OneHundred)

    override def getAllItems: List[PageSize] = allItems

    override def refresh(): Unit =
      myView.refreshPagination()

    override def isMultipleSelection: Boolean = false

    override def isSelected(item: PageSize): Boolean =
      myState.pageSize.value == item.value

    override def getSelectedItems: List[PageSize] = {
      val pageSize = myState.pageSize
      List(pageSize)
    }

    override def addSelectedItems(items: List[PageSize]): Unit = ()

    override def toggleSelection(item: PageSize): Unit =
      PageSize.fromInt(item.value).foreach { pageSize =>
        myState = myState.copy(pageSize = pageSize)
        if myState.currentPage * pageSize.value > myState.totalSize then myState = myState.copy(currentPage = 1)
        refreshQuery()
      }

    override def removeSelectedItems(items: List[PageSize]): Unit = ()

    override def getPageSize: Int =
      myState.pageSize.value

    override def getCurrentPage: Int = myState.currentPage

    override def setCurrentPage(page: Int): Unit =
      myState = myState.copy(currentPage = page)
      refreshQuery(false)

    override def getTotalPages: Int = math.ceil(myState.totalSize.toDouble / myState.pageSize.value).toInt

    override def getTotalItems: Int = myState.totalSize
  }

  private def createTagAction(
    id: String,
    text: String,
    icon: Option[Icon],
    radius: Float,
    onCloseAction: Option[() => Unit]
  ): AnAction = DefaultCustomComponentAction { () =>
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

    tagActionGroup.add(
      createTagAction(myState.selectedDomain.slug, myState.selectedDomain.name, None, DOMAIN_TAG_RADIUS, None)
    )

    myState.selectedDifficulties.foreach { difficulty =>
      tagActionGroup.add(
        createTagAction(
          difficulty.value,
          difficulty.showAsHtml,
          None,
          DIFFICULTY_TAG_RADIUS,
          Some(() => myDifficultiesProvider.removeSelectedItems(List(Difficulty(difficulty.show, difficulty.value))))
        )
      )
    }
    myState.selectedStatus.foreach { status =>
      tagActionGroup.add(
        createTagAction(
          status.value,
          status.show,
          None,
          STATUS_TAG_RADIUS,
          Some(() => myStatusProvider.removeSelectedItems(List(Status(status.show, status.value))))
        )
      )
    }

    myState.selectedSkills.foreach { skill =>
      tagActionGroup.add(
        createTagAction(
          skill.value,
          skill.show,
          None,
          SKILL_TAG_RADIUS,
          Some(() => mySkillProvider.removeSelectedItems(List(Skill(skill.show, skill.value))))
        )
      )
    }
    myState.selectedSubdomains.foreach { subdomain =>
      tagActionGroup.add(
        createTagAction(
          subdomain.slug,
          subdomain.name,
          None,
          SUBDOMAIN_TAG_RADIUS,
          Some(() =>
            myTagProvider.removeSelectedItems(List(Tag(subdomain.name, subdomain.slug, myState.selectedDomain.slug)))
          )
        )
      )
    }

    myView
      .refreshTagToolbar()
  }

  override def dispose(): Unit = {}

  def getComponent: JComponent = myView

  private def updateChallengeItems(items: List[ChallengeDetail]): Unit =
    myView.getTableModel.setItems(items.asJava)
}

object QueryParametersViewPresenter {
  private case class InitialData(userInfo: UserInfo, challengeDomains: List[ChallengeDomain])
  private case class State(
    selectedDomain: ChallengeDomain,
    selectedSubdomains: List[ChallengeSubdomain],
    selectedDifficulties: List[ChallengeDifficulty],
    selectedStatus: Option[ChallengeStatus],
    selectedSkills: List[ChallengeSkill],
    currentItems: List[ChallengeDetail] = Nil,
    currentPage: Int = 1,
    totalSize: Int = 1,
    pageSize: PageSize = PageSize.Twenty
  ) {
    def resetToFirstPage(): State = this.copy(currentPage = 1)
    def resetPagination(): State  = this.copy(currentPage = 1, totalSize = 1)
  }

  final private val EMPTY_STATE = State(PROJECT_EULER_DOMAIN, Nil, Nil, None, Nil, Nil, 1, 1, PageSize.Twenty)

  private val DOMAIN_TAG_RADIUS     = 0.2f
  private val DIFFICULTY_TAG_RADIUS = 0.4f
  private val STATUS_TAG_RADIUS     = 0.5f
  private val SKILL_TAG_RADIUS      = 0.6f
  private val SUBDOMAIN_TAG_RADIUS  = 1.0f
}
