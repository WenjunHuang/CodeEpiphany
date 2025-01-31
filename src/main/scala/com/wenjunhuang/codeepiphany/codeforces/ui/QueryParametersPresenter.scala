package com.wenjunhuang.codeepiphany.codeforces.ui

import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import javax.swing.JComponent
import org.jooq.impl.DSL
import org.typelevel.log4cats.{Logger, LoggerFactory}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.annotations.RequiresEdt

import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.{DIFFICULTIES_PROVIDER_KEY, DifficultyParameterProvider}
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.{CHALLENGE_PROVIDER_KEY, OpenChallengeProvider}
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.{PageSize, PAGINATION_PROVIDER_KEY, PaginationParameterProvider}
import com.wenjunhuang.codeepiphany.actions.RefreshAction.{REFRESH_PROVIDER_KEY, RefreshProvider}
import com.wenjunhuang.codeepiphany.actions.TagsAction
import com.wenjunhuang.codeepiphany.actions.TagsAction.*
import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesSearchOrderBy
import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesApi
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettings
import com.wenjunhuang.codeepiphany.codeforces.ui.QueryParametersPresenter.*
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.database.tables.records.CodeforcesProblemsetsRecord
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

class QueryParametersPresenter(private val myProject: Project)
    extends OrderDirectionProvider[CodeForcesSearchOrderBy]
    with Disposable {

  private implicit val myLogger: Logger[IO] = LoggerFactory[IO].getLogger

  private implicit val httpClientKeeper: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myApi = CodeForcesApi[IO]()

  private val myView = QueryParametersView(myProject, this)

  @volatile
  private var myInitialData = EMPTY_INITIAL_DATA
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
      .evalTap { _ => IO.delay { rebuildTags() }.evalOnEDTAny() }
      .evalTap { case (signal, state) =>
        val from  = math.max((state.currentPage - 1) * state.pageSize.value, 0)
        val limit = state.pageSize.value
        Stream
          .eval(
            ChallengeRepository
              .getInstance(myProject)
              .getDSLContextResource[IO]
              .use { dsl =>
                IO.delay {
                  val condition =
                    if state.selectedTags.isEmpty then DSL.trueCondition()
                    else
                      val matches = s"${CODEFORCES_PROBLEMSETS_FTS.TAGS.getUnqualifiedName}:${state.selectedTags
                          .map(tag => tag.value)
                          .mkString(" + ")}"
                      DSL.condition("{0} MATCH {1}", DSL.field(CODEFORCES_PROBLEMSETS_FTS.getUnqualifiedName), matches)
                  val total = dsl
                    .selectCount()
                    .from(CODEFORCES_PROBLEMSETS_FTS)
                    .where(condition)
                    .fetchOne(0, classOf[Int])

                  val orderBy = state.orderBy.map {
                    case (CodeForcesSearchOrderBy.Rating, dir) => dir.toJooqSortField(CODEFORCES_PROBLEMSETS_FTS.RATING)
                    case (CodeForcesSearchOrderBy.ContestIdIndex, dir) =>
                      dir.toJooqSortField(CODEFORCES_PROBLEMSETS_FTS.CONTESTIDINDEX)
                  }

                  val query = dsl
                    .selectFrom(CODEFORCES_PROBLEMSETS_FTS)
                    .where(condition)
                    .orderBy(orderBy.toList*)
                    .limit(from, limit)
                    .fetchInto(classOf[CodeforcesProblemsetsRecord])
                    .asScala
                    .toList

                  (total, query)
                }
              }
          )
          .evalMap { case (total, query) =>
            IO.delay {
              myState = state.copy(currentItems = query, totalSize = total)
              myPaginationProvider.refresh()
              updateChallengeItems(query)
            }.evalOnEDTAny()
          }
          .interruptWhen(signal)
          .onFinalizeCaseWeak(c =>
            myLogger.info(s"${CodeDojo.CodeForces.show} Query Parameter stream finalized, because of $c")
          )
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

  override def getDirectionOf(field: CodeForcesSearchOrderBy): Option[OrderDirection] = myState.orderBy.collect {
    case (f, d) if f == field => d
  }

  override def setDirectionOf(field: CodeForcesSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    myState = direction match
      case None            => myState.copy(orderBy = None)
      case Some(direction) => myState.copy(orderBy = Some((field, direction)))
    requery()
  }

  def initialize(): IO[InitialData] = {
    myApi.getProblemTags.map { tags =>
      myInitialData = InitialData(tags)
      myInitialData
    }
  }

  def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(DIFFICULTIES_PROVIDER_KEY, myDifficultiesProvider)
    dataSink.set(PAGINATION_PROVIDER_KEY, myPaginationProvider)
    dataSink.set(CHALLENGE_PROVIDER_KEY, myChallengeProvider)
    dataSink.set(REFRESH_PROVIDER_KEY, myRefreshProvider)
    dataSink.set(TAG_PROVIDER_KEY, myTagProvider)
  }

  private val myChallengeProvider = new OpenChallengeProvider {
    override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
      Option(myView.getTable.getSelectedObject) match {
        case Some(selected) =>
        case None => ()
      }
    }

    override def getLanguages: List[(Language, LanguageVersion)] = {
      CodeForcesSettings.getInstance(myProject).getSelectedLanguages
    }

    override def currentSelectedCanBeOpened: Boolean = true

  }

  private val myDifficultiesProvider = new DifficultyParameterProvider {
    override def isSelected(item: ChallengeDifficulty): Boolean =
      myState.selectedDifficulty.contains(item)

    override def isMultipleSelection: Boolean = false

    override def toggleSelection(item: ChallengeDifficulty): Unit = {
      if myState.selectedDifficulty.contains(item) then myState = myState.copy(selectedDifficulty = None)
      else myState = myState.copy(selectedDifficulty = Some(item))
      requery()
    }

    override def getAllItems: List[ChallengeDifficulty] =
      List(ChallengeDifficulty.Easy, ChallengeDifficulty.Medium, ChallengeDifficulty.Hard)

    override def getSelectedItems: List[ChallengeDifficulty] = myState.selectedDifficulty.toList

    override def addSelectedItems(items: List[ChallengeDifficulty]): Unit = {
      myState = myState.copy(selectedDifficulty = items.headOption)
      requery()
    }

    override def removeSelectedItems(items: List[ChallengeDifficulty]): Unit = {
      if myState.selectedDifficulty.contains(items.head) then
        myState = myState.copy(selectedDifficulty = None)
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

  private val myTagProvider = new SingleTagGroupProvider {
    override def getAllItems: List[Tag] = myInitialData.tags.map(t => Tag(t, t, t))

    override def isMultipleSelection: Boolean = true

    override def isSelected(item: Tag): Boolean = myState.selectedTags.contains(item)

    override def getSelectedItems: List[Tag] = myState.selectedTags

    override def addSelectedItems(items: List[Tag]): Unit = {
      myState = myState.copy(selectedTags = (myState.selectedTags ++ items).distinct)
      requery()
    }

    override def toggleSelection(item: Tag): Unit = {
      if myState.selectedTags.contains(item) then
        myState = myState.copy(selectedTags = myState.selectedTags.filterNot(_ == item))
      else myState = myState.copy(selectedTags = (myState.selectedTags :+ item).distinct)
      requery()
    }

    override def removeSelectedItems(items: List[Tag]): Unit = {
      myState = myState.copy(selectedTags = myState.selectedTags.filterNot(items.contains))
      requery()
    }
  }

  @RequiresEdt
  private def rebuildTags(): Unit = {
    val tagPane = myView.getTagPane
    tagPane.removeAllTags()

    myState.selectedDifficulty.foreach { difficulty =>
      tagPane.addClosableTagAction(
        difficulty.value,
        difficulty.showAsHtml,
        None,
        DIFFICULTY_TAG_RADIUS,
        Some(() => myDifficultiesProvider.removeSelectedItems(List(difficulty)))
      )
    }
    myState.selectedTags.foreach { tag =>
      tagPane.addClosableTagAction(
        tag.value,
        tag.name,
        None,
        TAG_TAG_RADIUS,
        Some(() => myTagProvider.removeSelectedItems(List(tag)))
      )
    }

    myView.refreshTagToolbar()
  }

  def requery(resetToFirstPage: Boolean = true): Unit =
    myQueryQueue.foreach { q =>
      val state = if resetToFirstPage then myState.resetToFirstPage() else myState
      q.offer(Some(state)).unsafeRunAndForget()
    }

  override def dispose(): Unit = {
    myQueryQueue.foreach(_.offer(None).unsafeRunSync())
  }

  def getComponent: JComponent = myView

  private def updateChallengeItems(items: List[CodeforcesProblemsetsRecord]): Unit =
    myView.getTableModel.setItems(items.asJava)
}

object QueryParametersPresenter {
  case class InitialData(tags: List[String] = Nil)

  case class QueryParams(
    selectedDifficulty: Option[ChallengeDifficulty],
    selectedTags: List[Tag],
    orderBy: Option[(CodeForcesSearchOrderBy, OrderDirection)],
    currentItems: List[CodeforcesProblemsetsRecord],
    currentPage: Int = 1,
    totalSize: Int = 1,
    pageSize: PageSize = PageSize.Twenty
  ) {
    def resetToFirstPage(): QueryParams = this.copy(currentPage = 1)
    def resetPagination(): QueryParams  = this.copy(currentPage = 1, totalSize = 1)
  }

  final val EMPTY_STATE: QueryParams = QueryParams(None, Nil, None, Nil, 1, 1, PageSize.Twenty)
  final val EMPTY_INITIAL_DATA       = InitialData(Nil)

  private val PROBLEMSET_TAG_RADIUS = 0.1f
  private val DIFFICULTY_TAG_RADIUS = 0.3f
  private val TAG_TAG_RADIUS        = 1.0f
}
