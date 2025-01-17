package com.wenjunhuang.codeepiphany.leetcode.ui

import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import javax.swing.JComponent
import org.typelevel.log4cats.{ Logger, LoggerFactory }
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.annotations.RequiresEdt

import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.{
  DIFFICULTIES_PROVIDER_KEY,
  DifficultyParameterProvider
}
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.{ CHALLENGE_PROVIDER_KEY, OpenChallengeProvider }
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.{
  PAGINATION_PROVIDER_KEY,
  PageSize,
  PaginationParameterProvider
}
import com.wenjunhuang.codeepiphany.actions.RefreshAction.{ REFRESH_PROVIDER_KEY, RefreshProvider }
import com.wenjunhuang.codeepiphany.actions.StatusParameterAction.{ STATUS_PROVIDER_KEY, StatusParameterProvider }
import com.wenjunhuang.codeepiphany.actions.TagsAction.*
import com.wenjunhuang.codeepiphany.actions.TagsAction
import com.wenjunhuang.codeepiphany.leetcode.actions.FavoriteParameterAction.*
import com.wenjunhuang.codeepiphany.leetcode.model.*
import com.wenjunhuang.codeepiphany.leetcode.services.{ LeetCodeApi, LeetCodeSearchOrderBy }
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

class QueryParametersPresenter(private val myProject: Project, private val myCodeDojo: CodeDojo)
    extends OrderDirectionProvider[LeetCodeSearchOrderBy]
    with Disposable {
  import QueryParametersPresenter.*

  implicit private val myLogger: Logger[IO] = LoggerFactory[IO].getLogger

  implicit private val httpClientKeeper: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager
  private val myApi = LeetCodeApi[IO](myCodeDojo)

  private val myView = QueryParametersView(myProject, this, myCodeDojo)

  @volatile
  private var myInitialData = InitialData(LeetCodeUserInfo.EMPTY_USERINFO)
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
                state.selectedFavorite,
                state.selectedDifficulty,
                state.selectedStatus,
                state.selectedTags.map(_.userObj.asInstanceOf[LeetCodeTag]),
                state.orderBy
              )
              .map { response => state.copy(currentItems = response.questions, totalSize = response.total) }
              .flatMap { state =>
                IO.delay {
                  myState = state
                  myPaginationProvider.refresh()
                  updateChallengeItems(state.currentItems)
                }.evalOnEDTAny()
              }
          )
          .interruptWhen(signal)
          .onFinalizeCaseWeak(c => myLogger.info(s"${myCodeDojo.show} Query Parameter stream finalized, because of $c"))
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

  myProject.getMessageBus
    .connect(this)
    .subscribe(
      messages.LOGIN_LOGOUT_TOPIC,
      new messages.LoginLogoutNotifier {
        override def login(codeDojo: CodeDojo): Unit = {
          if codeDojo == myCodeDojo then requery()
        }

        override def logout(codeDojo: CodeDojo): Unit =
          if codeDojo == myCodeDojo then
            myInitialData = InitialData(LeetCodeUserInfo.EMPTY_USERINFO)
            myState = EMPTY_STATE
      }
    )

  Disposer.register(myProject, this)

  override def getDirectionOf(field: LeetCodeSearchOrderBy): Option[OrderDirection] = myState.orderBy.collect {
    case (f, d) if f == field => d
  }

  override def setDirectionOf(field: LeetCodeSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    direction match
      case None            => myState = myState.copy(orderBy = None)
      case Some(direction) => myState = myState.copy(orderBy = Some((field, direction)))
    requery()
  }

  def getInitialData: IO[Unit] = {
    (myApi.getUserInfo(), myApi.getFavoriteList, myApi.getTagTypeWithTags).parMapN {
      (userInfo, favoriteItems, tagTypeWithTags) =>
        myInitialData = InitialData(userInfo, favoriteItems, tagTypeWithTags)
    }
  }

  def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(FAVORITE_PROVIDER_KEY, myFavoriteProvider)
    dataSink.set(DIFFICULTIES_PROVIDER_KEY, myDifficultiesProvider)
    dataSink.set(STATUS_PROVIDER_KEY, myStatusProvider)
    dataSink.set(PAGINATION_PROVIDER_KEY, myPaginationProvider)
    dataSink.set(CHALLENGE_PROVIDER_KEY, myChallengeProvider)
    dataSink.set(REFRESH_PROVIDER_KEY, myRefreshProvider)
    dataSink.set(TAG_PROVIDER_KEY, myTagProvider)
  }

  private val myChallengeProvider = new OpenChallengeProvider {
    override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
      Option(myView.getTable.getSelectedObject) match
        case Some(selected) =>
        case None           => ()
    }

    override def getLanguages: List[(Language, LanguageVersion)] = ???

  }

  private val myFavoriteProvider = new FavoriteParameterProvider {
    override def getAllItems: List[LeetCodeFavoriteItem] = myInitialData.favoriteItems

    override def isMultipleSelection: Boolean = false

    override def isSelected(item: LeetCodeFavoriteItem): Boolean = myState.selectedFavorite.exists(_ == item)

    override def getSelectedItems: List[LeetCodeFavoriteItem] = myState.selectedFavorite.toList

    override def addSelectedItems(items: List[LeetCodeFavoriteItem]): Unit = {
      myState = myState.copy(selectedFavorite = items.headOption)
      requery()
    }

    override def toggleSelection(item: LeetCodeFavoriteItem): Unit = {
      if myState.selectedFavorite.contains(item) then myState = myState.copy(selectedFavorite = None)
      else myState = myState.copy(selectedFavorite = Some(item))
      requery()
    }

    override def removeSelectedItems(items: List[LeetCodeFavoriteItem]): Unit = {
      if myState.selectedFavorite.contains(items.head) then
        myState = myState.copy(selectedFavorite = None)
        requery()
    }
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

  private val myStatusProvider = new StatusParameterProvider {
    private val allItems = List(ChallengeStatus.Solved, ChallengeStatus.Unsolved, ChallengeStatus.Tried)

    override def isSelected(item: ChallengeStatus): Boolean = myState.selectedStatus.contains(item)

    override def getAllItems: List[ChallengeStatus] = allItems

    override def isMultipleSelection: Boolean = false

    override def getSelectedItems: List[ChallengeStatus] = myState.selectedStatus.toList

    override def addSelectedItems(items: List[ChallengeStatus]): Unit = {
      myState = myState.copy(selectedStatus = items.headOption)
      requery()
    }

    override def toggleSelection(item: ChallengeStatus): Unit =
      if myState.selectedStatus.contains(item) then myState = myState.copy(selectedStatus = None)
      else myState = myState.copy(selectedStatus = allItems.find(_.value == item.value))
      requery()

    override def removeSelectedItems(items: List[ChallengeStatus]): Unit = {
      if myState.selectedStatus.contains(items.head) then
        myState = myState.copy(selectedStatus = None)
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

  private val myTagProvider = new MultiTagGroupProvider {
    private val allTags = myInitialData.tagTypeWithTags.flatMap { item =>
      item.tagRelation.map { relation =>
        Tag(relation.tag.nameTranslated.getOrElse(relation.tag.name), relation.tag.slug, item.name, relation.tag)
      }
    }

    override def isSearchEnabled: Boolean = true

    override def searchTags(query: String): List[Tag] = Nil

    override def getTabs: List[TagsAction.TagGroupTab] = {
      val groups = myInitialData.tagTypeWithTags.map { item =>
        TagGroup(
          item.transName.getOrElse(item.name),
          item.name,
          item.tagRelation.map { relation =>
            Tag(relation.tag.nameTranslated.getOrElse(relation.tag.name), relation.tag.slug, item.name, relation.tag)
          },
          item
        )
      }
      List(TagGroupTab("Tags", "tags", groups))
    }

    override def getAllItems: List[Tag] = allTags

    override def isMultipleSelection: Boolean = true

    override def isSelected(item: Tag): Boolean = {
      myState.selectedTags.contains(item)
    }

    override def getSelectedItems: List[Tag] = myState.selectedTags

    override def addSelectedItems(items: List[Tag]): Unit = {
      myState = myState.copy(selectedTags = (myState.selectedTags ++ items).distinct)
      requery()
    }

    override def toggleSelection(item: Tag): Unit = {
      if myState.selectedTags.contains(item) then
        myState = myState.copy(selectedTags =
          myState.selectedTags.filterNot(_ == item.userObj.asInstanceOf[LeetCodeTag]).distinct
        )
      else myState = myState.copy(selectedTags = (myState.selectedTags ++ List(item)).distinct)
      requery()
    }

    override def removeSelectedItems(items: List[Tag]): Unit = {
      myState = myState.copy(selectedTags = myState.selectedTags.filterNot(items.contains).distinct)
      requery()
    }
  }

  @RequiresEdt
  private def refreshTags(): Unit = {
    val tagPane = myView.getTagPane
    tagPane.removeAllTags()

    myState.selectedFavorite.foreach { favorite =>
      tagPane.addClosableTagAction(
        favorite.id,
        favorite.name,
        None,
        FAVORITE_TAG_RADIUS,
        Some(() => myFavoriteProvider.removeSelectedItems(List(favorite)))
      )
    }

    myState.selectedDifficulty.foreach { difficulty =>
      tagPane.addClosableTagAction(
        difficulty.value,
        difficulty.showAsHtml,
        None,
        DIFFICULTY_TAG_RADIUS,
        Some(() => myDifficultiesProvider.removeSelectedItems(List(difficulty)))
      )
    }
    myState.selectedStatus.foreach { status =>
      tagPane.addClosableTagAction(
        status.value,
        status.show,
        None,
        STATUS_TAG_RADIUS,
        Some(() => myStatusProvider.removeSelectedItems(List(status)))
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

  private def requery(resetToFirstPage: Boolean = true): Unit =
    myQueryQueue.foreach { q =>
      val state = if resetToFirstPage then myState.resetToFirstPage() else myState
      q.offer(Some(state)).unsafeRunAndForget()
    }

  override def dispose(): Unit = {
    myQueryQueue.foreach(_.offer(None).unsafeRunSync())
  }

  def getComponent: JComponent = myView

  private def updateChallengeItems(items: List[LeetCodeChallengeListItem]): Unit =
    myView.getTableModel.setItems(items.asJava)
}

object QueryParametersPresenter {
  private case class InitialData(
    userInfo: LeetCodeUserInfo,
    favoriteItems: List[LeetCodeFavoriteItem] = Nil,
    tagTypeWithTags: List[LeetCodeTagTypeWithTags] = Nil
  )

  private case class QueryParams(
    selectedFavorite: Option[LeetCodeFavoriteItem],
    selectedDifficulty: Option[ChallengeDifficulty],
    selectedStatus: Option[ChallengeStatus],
    selectedTags: List[Tag],
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)],
    currentItems: List[LeetCodeChallengeListItem],
    currentPage: Int = 1,
    totalSize: Int = 1,
    pageSize: PageSize = PageSize.Twenty
  ) {
    def resetToFirstPage(): QueryParams = this.copy(currentPage = 1)
    def resetPagination(): QueryParams  = this.copy(currentPage = 1, totalSize = 1)
  }

  final private val EMPTY_STATE = QueryParams(None, None, None, Nil, None, Nil, 1, 1, PageSize.Twenty)

  private val FAVORITE_TAG_RADIUS   = 0.2f
  private val DIFFICULTY_TAG_RADIUS = 0.4f
  private val STATUS_TAG_RADIUS     = 0.5f
  private val TAG_TAG_RADIUS        = 1.0f
}
