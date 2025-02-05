package com.wenjunhuang.codeepiphany.hackerrank.ui

import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import javax.swing.JComponent
import org.typelevel.ci.CIString
import org.typelevel.log4cats.{Logger, LoggerFactory}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.{AtomicBooleanProperty, AtomicProperty}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.annotations.RequiresEdt

import com.wenjunhuang.codeepiphany.utils.PageSize
import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.{DIFFICULTIES_PROVIDER_KEY, DifficultyParameterProvider}
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.{CHALLENGE_PROVIDER_KEY, OpenChallengeProvider}
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.{PAGINATION_PROVIDER_KEY, PaginationParameterProvider}
import com.wenjunhuang.codeepiphany.actions.RefreshAction.{REFRESH_PROVIDER_KEY, RefreshProvider}
import com.wenjunhuang.codeepiphany.actions.StatusParameterAction.{STATUS_PROVIDER_KEY, StatusParameterProvider}
import com.wenjunhuang.codeepiphany.actions.TagsAction.{SingleTagGroupProvider, Tag, TAG_PROVIDER_KEY}
import com.wenjunhuang.codeepiphany.hackerrank.actions.CategoryParameterAction.{Category, CATEGORY_PROVIDER_KEY, CategoryProvider}
import com.wenjunhuang.codeepiphany.hackerrank.actions.SkillParameterAction.{SKILL_PROVIDER_KEY, SkillParameterProvider}
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.hackerrank.services.{HackerRankApi, HackerRankOpenChallengeRequest, HackerRankOpenChallengeService}
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

class QueryParametersPresenter(private val myProject: Project) extends Disposable {
  import QueryParametersPresenter.*

  private implicit val myLogger: Logger[IO] = LoggerFactory[IO].getLogger
  private val myTagsActionModel             = AtomicProperty[List[TagPaneAction]](Nil)

  private implicit val httpClientKeeper: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager
  private val myApi = HackerRankApi[IO]()

  private val myView = QueryParametersView(myProject, this)

  @volatile
  private var myInitialData = InitialData(EMPTY_USERINFO, Nil)
  @volatile
  private var myState: QueryParams = EMPTY_STATE

  @volatile
  private var myQueryQueue: Option[Queue[IO, Option[QueryParams]]] = None

  private val myQueryWorker = for {
    q              <- Queue.unbounded[IO, Option[QueryParams]]
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
      .evalTap { _ => IO.delay { refreshTags() }.evalOnEDTAny() }
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
          .onFinalizeCaseWeak(c => myLogger.debug(s"HackerRank Query Parameter stream finalized, because of $c"))
          .compile
          .drain
          .evalAsBackgroundProgress(myProject, "Querying challenges...")
          .attempt
      }
      .onFinalize(myLogger.info("Query worker is finalized"))
      .compile
      .drain
  } yield ()

  myQueryWorker.unsafeRunAndForget()

  Disposer.register(myProject, this)

  def initialize(): IO[Unit] = {
    myApi.getInitialData.map { case (userInfo, challengeDomains) =>
      myInitialData = InitialData(userInfo, challengeDomains.sortBy(_.id) :+ PROJECT_EULER_DOMAIN)
      myState = EMPTY_STATE.copy(selectedDomain = myInitialData.challengeDomains.head)
      requery()
    }
  }

  def getTagsActionModel = myTagsActionModel

  def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(CATEGORY_PROVIDER_KEY, myCategoryProvider)
    dataSink.set(DIFFICULTIES_PROVIDER_KEY, myDifficultiesProvider)
    dataSink.set(STATUS_PROVIDER_KEY, myStatusProvider)
    dataSink.set(SKILL_PROVIDER_KEY, mySkillProvider)
    dataSink.set(TAG_PROVIDER_KEY, myTagProvider)
    dataSink.set(PAGINATION_PROVIDER_KEY, myPaginationProvider)
    dataSink.set(CHALLENGE_PROVIDER_KEY, myChallengeProvider)
    dataSink.set(REFRESH_PROVIDER_KEY, myRefreshProvider)
  }

  private val myChallengeProvider = new OpenChallengeProvider {
    override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
      Option(myView.getTable.getSelectedObject) match
        case Some(selected) =>
          HackerRankOpenChallengeService[IO](myProject)
            .openChallenge(
              HackerRankOpenChallengeRequest(
                selected.slug,
                HackerRankContest.fromCIString(CIString(selected.contestSlug)).get
              ),
              language,
              languageVersion
            )
            .handleErrorWith(e =>
              console.error[IO](myProject, e.getMessage) *> myLogger.warn(e)(
                s"Failed to open challenge ${selected.slug}"
              )
            )
            .evalAsBackgroundProgress(myProject, s"Opening HackerRank challenge '${selected.name}'...")
            .unsafeRunAndForget()

        case None => ()
    }

    override def getLanguages: List[(Language, LanguageVersion)] = {
      val settings = HackerRankSettings.getInstance(myProject)
      settings.getSelectedLanguages
    }

    override def currentSelectedCanBeOpened: Boolean = true
  }

  private val myCategoryProvider = new CategoryProvider {
    override def isSelected(item: Category): Boolean =
      myState.selectedDomain.slug == item.value

    override def toggleSelection(item: Category): Unit =
      if myState.selectedDomain.slug != item.value && myInitialData.challengeDomains.exists(_.slug == item.value) then
        myInitialData.challengeDomains.find(_.slug == item.value).foreach { newSelected =>
          myState = myState.copy(selectedDomain = newSelected, selectedSubdomains = Nil).resetPagination()
          requery()
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
            requery()
        case _ =>

    override def removeSelectedItems(items: List[Category]): Unit = {}
  }

  private val myDifficultiesProvider = new DifficultyParameterProvider {
    override def isSelected(item: ChallengeDifficulty): Boolean =
      myState.selectedDifficulties.contains(item)

    override def isMultipleSelection: Boolean = true

    override def toggleSelection(item: ChallengeDifficulty): Unit =
      if myState.selectedDifficulties.contains(item) then
        myState = myState.copy(selectedDifficulties = myState.selectedDifficulties.filterNot(_ == item))
      else myState = myState.copy(selectedDifficulties = myState.selectedDifficulties :+ item)

    override def getAllItems: List[ChallengeDifficulty] =
      List(ChallengeDifficulty.Easy, ChallengeDifficulty.Medium, ChallengeDifficulty.Hard)

    override def getSelectedItems: List[ChallengeDifficulty] =
      myState.selectedDifficulties

    override def addSelectedItems(items: List[ChallengeDifficulty]): Unit =
      myState = myState.copy(selectedDifficulties = (myState.selectedDifficulties ++ items).distinct)
      requery()

    override def removeSelectedItems(items: List[ChallengeDifficulty]): Unit =
      myState = myState.copy(selectedDifficulties = myState.selectedDifficulties.filterNot(items.contains))
      requery()
  }

  private val myStatusProvider = new StatusParameterProvider {
    private val allItems = List(ChallengeStatus.Solved, ChallengeStatus.Unsolved)

    override def isSelected(item: ChallengeStatus): Boolean =
      myState.selectedStatus.exists(_.value == item.value)

    override def getAllItems: List[ChallengeStatus] = allItems

    override def isMultipleSelection: Boolean = false

    override def getSelectedItems: List[ChallengeStatus] =
      myState.selectedStatus.toList

    override def addSelectedItems(items: List[ChallengeStatus]): Unit = {
      myState =
        myState.copy(selectedStatus = items.headOption.flatMap(status => allItems.find(_.value == status.value)))
      requery()
    }

    override def toggleSelection(item: ChallengeStatus): Unit =
      if myState.selectedStatus.exists(_.value == item.value) then myState = myState.copy(selectedStatus = None)
      else myState = myState.copy(selectedStatus = allItems.find(_.value == item.value))
      requery()

    override def removeSelectedItems(items: List[ChallengeStatus]): Unit =
      if myState.selectedStatus.exists(_.value == items.head.value) then
        myState = myState.copy(selectedStatus = None)
        requery()
  }

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
        case Tag(_, value, groupValue, _)
            if myState.selectedDomain.slug == groupValue && myState.selectedDomain.subDomains.exists(_.slug == value) =>
          myState.selectedDomain.subDomains.find(_.slug == value).get
      }).distinct)
      requery()

    override def removeSelectedItems(items: List[Tag]): Unit =
      myState = myState.copy(selectedSubdomains =
        myState.selectedSubdomains.filterNot(subdomain => items.exists(_.value == subdomain.slug))
      )
      requery()

    override def toggleSelection(item: Tag): Unit =
      if isSelected(item) then removeSelectedItems(List(item))
      else addSelectedItems(List(item))
  }

  private val mySkillProvider = new SkillParameterProvider {
    private val allItems: List[HackerRankChallengeSkill] =
      List(HackerRankChallengeSkill.Basic, HackerRankChallengeSkill.Intermediate, HackerRankChallengeSkill.Advanced)
    override def getAllItems: List[HackerRankChallengeSkill] = allItems

    override def isMultipleSelection: Boolean = true

    override def isSelected(item: HackerRankChallengeSkill): Boolean =
      myState.selectedSkills.exists(_.value == item.value)

    override def getSelectedItems: List[HackerRankChallengeSkill] =
      myState.selectedSkills

    override def addSelectedItems(items: List[HackerRankChallengeSkill]): Unit = {
      myState = myState.copy(selectedSkills = (myState.selectedSkills ++ items).distinct)
      requery()
    }

    override def toggleSelection(item: HackerRankChallengeSkill): Unit = {
      if myState.selectedSkills.exists(_.value == item.value) then
        myState = myState.copy(selectedSkills = myState.selectedSkills.filterNot(_.value == item.value))
      else myState = myState.copy(selectedSkills = myState.selectedSkills :+ item)
      requery()
    }

    override def removeSelectedItems(items: List[HackerRankChallengeSkill]): Unit = {
      myState = myState.copy(selectedSkills = myState.selectedSkills.filterNot(items.contains))
      requery()
    }

  }

  private val myPaginationProvider = new PaginationParameterProvider {
    private val allItems = List(PageSize.Twenty, PageSize.Fifty, PageSize.OneHundred)

    override def getAllItems: List[PageSize] = allItems

    override def refresh(): Unit =
      ApplicationManager.getApplication.invokeLater(() => myView.refreshPagination())

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
        requery()
      }

    override def removeSelectedItems(items: List[PageSize]): Unit = ()

    override def getPageSize: Int =
      myState.pageSize.value

    override def getCurrentPage: Int = myState.currentPage

    override def setCurrentPage(page: Int): Unit =
      myState = myState.copy(currentPage = page)
      requery(false)

    override def getTotalPages: Int = math.ceil(myState.totalSize.toDouble / myState.pageSize.value).toInt

    override def getTotalItems: Int = myState.totalSize
  }

  private val myRefreshProvider = new RefreshProvider {
    override def refresh(): Unit =
      requery()
  }

  @RequiresEdt
  private def refreshTags(): Unit = {
    val tags = List(
      TagPaneAction(myState.selectedDomain.slug, myState.selectedDomain.name, None, DOMAIN_TAG_RADIUS, None, None)
    ) ++
      myState.selectedDifficulties.map { difficulty =>
        TagPaneAction(
          difficulty.value,
          difficulty.showAsHtml,
          None,
          DIFFICULTY_TAG_RADIUS,
          None,
          Some(() => myDifficultiesProvider.removeSelectedItems(List(difficulty)))
        )
      } ++
      myState.selectedStatus.map { status =>
        TagPaneAction(
          status.value,
          status.show,
          None,
          STATUS_TAG_RADIUS,
          None,
          Some(() => myStatusProvider.removeSelectedItems(List(status)))
        )
      } ++
      myState.selectedSkills.map { skill =>
        TagPaneAction(
          skill.value,
          skill.show,
          None,
          SKILL_TAG_RADIUS,
          None,
          Some(() => mySkillProvider.removeSelectedItems(List(skill)))
        )
      } ++
      myState.selectedSubdomains.map { subdomain =>
        TagPaneAction(
          subdomain.slug,
          subdomain.name,
          None,
          SUBDOMAIN_TAG_RADIUS,
          None,
          Some(() =>
            myTagProvider.removeSelectedItems(List(Tag(subdomain.name, subdomain.slug, myState.selectedDomain.slug)))
          )
        )
      }
    myTagsActionModel.set(tags)

  }

  private def requery(resetToFirstPage: Boolean = true): Unit =
    myQueryQueue.foreach { q =>
      val state = if resetToFirstPage then myState.resetToFirstPage() else myState
      q.offer(Some(state)).unsafeRunAndForget()
    }

  override def dispose(): Unit = {
    myQueryQueue.foreach(_.offer(None).unsafeRunSync())
  }

  def getComponent: JComponent = myView

  private def updateChallengeItems(items: List[HackerRankChallengeDetail]): Unit =
    myView.getTableModel.setItems(items.asJava)
}

object QueryParametersPresenter {
  private case class InitialData(userInfo: HackerRankUserInfo, challengeDomains: List[HackerRankChallengeDomain])

  private case class QueryParams(
    selectedDomain: HackerRankChallengeDomain,
    selectedSubdomains: List[HackerRankChallengeSubdomain],
    selectedDifficulties: List[ChallengeDifficulty],
    selectedStatus: Option[ChallengeStatus],
    selectedSkills: List[HackerRankChallengeSkill],
    currentItems: List[HackerRankChallengeDetail] = Nil,
    currentPage: Int = 1,
    totalSize: Int = 1,
    pageSize: PageSize = PageSize.Twenty
  ) {
    def resetToFirstPage(): QueryParams = this.copy(currentPage = 1)
    def resetPagination(): QueryParams  = this.copy(currentPage = 1, totalSize = 1)
  }

  private final val EMPTY_STATE = QueryParams(PROJECT_EULER_DOMAIN, Nil, Nil, None, Nil, Nil, 1, 1, PageSize.Twenty)

  private val DOMAIN_TAG_RADIUS     = 0.2f
  private val DIFFICULTY_TAG_RADIUS = 0.4f
  private val STATUS_TAG_RADIUS     = 0.5f
  private val SKILL_TAG_RADIUS      = 0.6f
  private val SUBDOMAIN_TAG_RADIUS  = 1.0f
}
