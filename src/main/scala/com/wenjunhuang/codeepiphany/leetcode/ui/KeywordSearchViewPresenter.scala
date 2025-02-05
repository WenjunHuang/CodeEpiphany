package com.wenjunhuang.codeepiphany.leetcode.ui

import cats.effect.IO
import cats.effect.std.Queue
import fs2.Stream
import fs2.concurrent.SignallingRef
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import org.typelevel.log4cats.{Logger, LoggerFactory}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.{CHALLENGE_PROVIDER_KEY, OpenChallengeProvider}
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.{PAGINATION_PROVIDER_KEY, PaginationParameterProvider}
import com.wenjunhuang.codeepiphany.actions.RefreshAction.{REFRESH_PROVIDER_KEY, RefreshProvider}
import com.wenjunhuang.codeepiphany.leetcode.model.LeetCodeChallengeListItem
import com.wenjunhuang.codeepiphany.leetcode.services.{LeetCodeApi, LeetCodeOpenChallengeRequest, LeetCodeOpenChallengeService, LeetCodeSearchOrderBy}
import com.wenjunhuang.codeepiphany.leetcode.settings.{LeetCodeCNSettings, LeetCodeSettings}
import com.wenjunhuang.codeepiphany.leetcode.ui.KeywordSearchViewPresenter.SearchParam
import com.wenjunhuang.codeepiphany.model.CodeDojo.{LeetCode, LeetCodeCN}
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion, OrderDirection, OrderDirectionProvider}
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import cats.syntax.all.*

import com.wenjunhuang.codeepiphany.utils.PageSize

class KeywordSearchViewPresenter(
  private val myProject: Project,
  private val myCodeDojo: LeetCode.type | LeetCodeCN.type
) extends DocumentAdapter
    with OrderDirectionProvider[LeetCodeSearchOrderBy]
    with Disposable {
  private implicit val myLogger: Logger[IO] = LoggerFactory[IO].getLogger

  private implicit val httpClientKeeper: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myApi = LeetCodeApi[IO](myCodeDojo)

  private val myView: KeywordSearchView = KeywordSearchView(myProject, this, myCodeDojo)

  private var myState = KeywordSearchViewPresenter.EMPTY_PARAM

  @volatile
  private var myQueue: Option[Queue[IO, Option[SearchParam]]] = None

  private val mySearchStream = for {
    queue          <- Queue.unbounded[IO, Option[SearchParam]]
    _              <- IO.delay { myQueue = Some(queue) }
    notInterrupted <- SignallingRef.of[IO, Boolean](false)
    _ <- Stream
      .fromQueueNoneTerminated(queue)
      .evalMapAccumulate(notInterrupted) { case (signal, keyword) =>
        for {
          _         <- signal.set(true)
          newSignal <- SignallingRef.of[IO, Boolean](false)
        } yield (newSignal, keyword)
      }
      .debounce(200.millis)
      .evalTap { case (signal, state @ SearchParam(keyword, orderBy, currentPage, pageSize, _, _)) =>
        Stream
          .eval(
            myApi
              .searchChallengesWithKeyword(pageSize.value * (currentPage - 1), pageSize.value, keyword, orderBy)
              .map { response => state.copy(currentItems = response.questions, totalSize = response.total) }
              .flatMap { state =>
                IO.delay {
                  myState = state
                  myPaginationProvider.refresh()
                  updateChallenges(state.currentItems)
                }.evalOnEDTAny()
              }
          )
          .interruptWhen(signal)
          .attempt
          .onFinalizeCaseWeak(c => myLogger.debug(s"HackerRank Keyword search stream finalized, because of $c"))
          .compile
          .drain
          .evalAsBackgroundProgress(myProject, "Searching challenges...")
          .attempt
      }
      .onFinalize(myLogger.info("Search by keyword stream finalized"))
      .compile
      .drain
  } yield ()
  mySearchStream.unsafeRunAndForget()

  private def requery(resetToFirstPage: Boolean = true): Unit =
    myQueue.foreach { q =>
      val state = if resetToFirstPage then myState.resetToFirstPage() else myState
      q.offer(Some(state)).unsafeRunAndForget()
    }

  private val myChallengeProvider = new OpenChallengeProvider {
    override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
      Option(myView.getTable.getSelectedObject) match
        case Some(selected) =>
          LeetCodeOpenChallengeService[IO](myProject, myCodeDojo)
            .openChallenge(LeetCodeOpenChallengeRequest(selected.titleSlug), language, languageVersion)
            .handleErrorWith(e =>
              console.error[IO](myProject, e.getMessage) *>
                myLogger.error(e)("Failed to open challenge")
            )
            .evalAsBackgroundProgress(myProject, s"Opening  ${myCodeDojo.show} challenge ${selected.title}...")
            .unsafeRunAndForget()
        case None => ()
    }

    override def getLanguages: List[(Language, LanguageVersion)] = {
      myCodeDojo match
        case CodeDojo.LeetCode =>
          val settings = LeetCodeSettings.getInstance(myProject)
          settings.getSelectedLanguages
        case CodeDojo.LeetCodeCN =>
          val settings = LeetCodeCNSettings.getInstance(myProject)
          settings.getSelectedLanguages
    }

    override def currentSelectedCanBeOpened: Boolean = {
      Option(myView.getTable.getSelectedObject) match
        case None => false
        case Some(selected) =>
          !selected.paidOnly || (selected.paidOnly && myInitialData.userInfo.isPremium.contains(true))
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

  Disposer.register(myProject, this)

  @volatile
  private var myInitialData = QueryParametersPresenter.EMPTY_INITIAL_DATA

  def setInitialData(data: QueryParametersPresenter.InitialData): Unit = {
    myInitialData = data
    myView.getTableModel.userIsPremium = data.userInfo.isPremium.contains(true)
  }

  def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(CHALLENGE_PROVIDER_KEY, myChallengeProvider)
    dataSink.set(PAGINATION_PROVIDER_KEY, myPaginationProvider)
    dataSink.set(REFRESH_PROVIDER_KEY, myRefreshProvider)
  }

  def getComponent: JComponent = myView

  private def updateChallenges(challenges: List[LeetCodeChallengeListItem]): Unit =
    myView.getTableModel.setItems(challenges.asJava)

  override def getDirectionOf(v: LeetCodeSearchOrderBy): Option[OrderDirection] = myState.orderBy.collect {
    case (orderBy, direction) if orderBy == v => direction
  }

  override def setDirectionOf(v: LeetCodeSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    direction match
      case None            => myState = myState.copy(orderBy = None)
      case Some(direction) => myState = myState.copy(orderBy = Some((v, direction)))
    requery()
  }

  override def textChanged(e: DocumentEvent): Unit = {
    val keyword = e.getDocument.getText(0, e.getDocument.getLength)
    if keyword.nonEmpty then
      myQueue.foreach(_.offer(Some(myState.copy(keyword = keyword).resetToFirstPage())).unsafeRunAndForget())
  }

  override def dispose(): Unit =
    myQueue.foreach(_.offer(None).unsafeRunSync())
}
object KeywordSearchViewPresenter {
  case class SearchParam(
    keyword: String,
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)],
    currentPage: Int = 1,
    pageSize: PageSize = PageSize.Twenty,
    totalSize: Int = 1,
    currentItems: List[LeetCodeChallengeListItem] = Nil
  ) {
    def resetToFirstPage(): SearchParam = this.copy(currentPage = 1)
    def resetPagination(): SearchParam  = this.copy(currentPage = 1, totalSize = 1)
  }

  private val EMPTY_PARAM = SearchParam("", None)
}
