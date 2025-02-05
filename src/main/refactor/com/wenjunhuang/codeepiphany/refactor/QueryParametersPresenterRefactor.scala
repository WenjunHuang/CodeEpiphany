package com.wenjunhuang.codeepiphany.refactor

import cats.effect.{ IO, Ref, Resource }
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import java.util
import javax.swing.JComponent
import org.typelevel.ci.CIString
import org.typelevel.log4cats.{ Logger, LoggerFactory }
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import javax.swing.ListSelectionModel

import com.intellij.openapi.observable.properties.{ AtomicProperty, ObservableProperty }
import com.intellij.codeInspection.ui.ListTable
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.{ ActionGroup, AnAction, DataKey, DataSink, DefaultActionGroup }
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SingleSelectionModel
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.{ ColumnInfo, ListTableModel }
import scala.jdk.CollectionConverters.*

import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.{
  DIFFICULTIES_PROVIDER_KEY,
  DifficultyParameterProvider
}
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.{ CHALLENGE_PROVIDER_KEY, OpenChallengeProvider }
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.{
  PAGINATION_PROVIDER_KEY,
  PaginationParameterProvider
}
import com.wenjunhuang.codeepiphany.actions.RefreshAction.{ REFRESH_PROVIDER_KEY, RefreshProvider }
import com.wenjunhuang.codeepiphany.actions.RefreshAction
import com.wenjunhuang.codeepiphany.actions.StatusParameterAction.{ STATUS_PROVIDER_KEY, StatusParameterProvider }
import com.wenjunhuang.codeepiphany.actions.TagsAction.{ SingleTagGroupProvider, TAG_PROVIDER_KEY, Tag }
import com.wenjunhuang.codeepiphany.hackerrank.actions.CategoryParameterAction.{
  CATEGORY_PROVIDER_KEY,
  Category,
  CategoryProvider
}
import com.wenjunhuang.codeepiphany.hackerrank.actions.SkillParameterAction.{
  SKILL_PROVIDER_KEY,
  SkillParameterProvider
}
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.hackerrank.services.{
  HackerRankApi,
  HackerRankOpenChallengeRequest,
  HackerRankOpenChallengeService
}
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.refactor.QueryParametersViewRefactor
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.actions.ParameterProvider
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.PageSize
import com.wenjunhuang.codeepiphany.utils.Pagination
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

case class QueryContext[T](criteria: T, pagination: Pagination) {
  def updateCriteria(f: T => T): QueryContext[T] =
    this.copy(criteria = f(criteria))

  def updatePagination(f: Pagination => Pagination): QueryContext[T] =
    this.copy(pagination = f(pagination))

  def resetPagination: QueryContext[T] =
    this.copy(pagination = pagination.copy(currentPage = 1, totalSize = 0))

  def resetToFirstPage: QueryContext[T] =
    this.copy(pagination = pagination.resetToFirstPage)
}

abstract class QueryParametersPresenterRefactor[UIBoostrapParameters, Criteria, ResultItem](implicit
  private val myProject: Project,
  private val myBoostrapParameters: UIBoostrapParameters
) extends Disposable {

  private implicit val myLogger: Logger[IO] = LoggerFactory[IO].getLogger

  private val myQueryStateManager     = QueryStateManager(createInitialQueryParameters(myBoostrapParameters))
  private val myQueryResultTableModel = ListTableModel[ResultItem]()
  myQueryResultTableModel.setColumnInfos(getQueryResultColumns.asInstanceOf[Array[ColumnInfo[?, ?]]])

  private val myQueryResultSelectionModel  = SingleSelectionModel()
  private val myView                       = QueryParametersViewRefactor[ResultItem](this)
  private val myQueryParametersActionGroup = DefaultActionGroup()
  private val myTagsActionModel            = AtomicProperty[List[TagPaneAction]](Nil)
  @volatile
  private var myQueryQueue: Option[Queue[IO, Option[QueryContext[Criteria]]]] = None
  createQueryPipeline()

  Disposer.register(myProject, this)

  def uiDataSnapshot(dataSink: DataSink): Unit = {
    val actions = prepareProviders(myQueryStateManager.get, { cb => myQueryStateManager.update(cb) }, dataSink)
    dataSink.set(PAGINATION_PROVIDER_KEY, myPaginationProvider)
    dataSink.set(REFRESH_PROVIDER_KEY, myRefreshProvider)
    myQueryParametersActionGroup.removeAll()
    myQueryParametersActionGroup.addAll(actions*)
    myQueryParametersActionGroup.add(RefreshAction())
  }

  protected def createInitialQueryParameters(boostrapParameters: UIBoostrapParameters): QueryContext[Criteria]
  protected def prepareProviders(
    context: QueryContext[Criteria],
    updater: (QueryContext[Criteria] => QueryContext[Criteria]) => Unit,
    dataSink: DataSink
  ): List[AnAction]
  protected def executeQuery(context: QueryContext[Criteria]): IO[(Pagination, List[ResultItem])]
  protected def getQueryResultColumns: Array[ColumnInfo[ResultItem, ?]]
  protected def createQueryParametersTags(
    context: QueryContext[Criteria],
    onCloseUpdater: (QueryContext[Criteria] => QueryContext[Criteria]) => Unit
  ): List[TagPaneAction]

  def getParametersActionGroup: ActionGroup                       = myQueryParametersActionGroup
  def getQueryResultTableModel: ListTableModel[ResultItem]        = myQueryResultTableModel
  def getQueryResultTableSelectionModel: ListSelectionModel       = myQueryResultSelectionModel
  def getTagsActionModel: ObservableProperty[List[TagPaneAction]] = myTagsActionModel

  private val myPaginationProvider = new PaginationParameterProvider {
    private val allItems = List(PageSize.Twenty, PageSize.Fifty, PageSize.OneHundred)

    override def getAllItems: List[PageSize] = allItems

    override def refresh(): Unit =
      ApplicationManager.getApplication.invokeLater(() => myView.refreshPagination())

    override def isMultipleSelection: Boolean = false

    override def isSelected(item: PageSize): Boolean =
      myQueryStateManager.get.pagination.pageSize == item

    override def getSelectedItems: List[PageSize] = {
      val pageSize = myQueryStateManager.get.pagination.pageSize
      List(pageSize)
    }

    override def addSelectedItems(items: List[PageSize]): Unit = {
      items.headOption.foreach { item =>
        toggleSelection(item)
      }
    }

    override def toggleSelection(item: PageSize): Unit = {
      if !getSelectedItems.contains(item) then
        val queryContext = myQueryStateManager.update(_.updatePagination(_.copy(pageSize = item)))
        if queryContext.pagination.currentPage * item.value > queryContext.pagination.totalSize then
          myQueryStateManager.update(_.resetToFirstPage)
        requery()
    }

    override def removeSelectedItems(items: List[PageSize]): Unit = ()

    override def getPageSize: Int =
      myQueryStateManager.get.pagination.pageSize.value

    override def getCurrentPage: Int = myQueryStateManager.get.pagination.currentPage

    override def setCurrentPage(page: Int): Unit = {
      if page >= 1 && page <= getTotalPages then
        myQueryStateManager.update(_.updatePagination(_.copy(currentPage = page)))
        requery()
    }

    override def getTotalPages: Int =
      myQueryStateManager.get.pagination.totalPages

    override def getTotalItems: Int = myQueryStateManager.get.pagination.totalSize
  }

  private val myRefreshProvider = new RefreshProvider {
    override def refresh(): Unit =
      requery()
  }

  private def requery(): Unit = {
    val state = myQueryStateManager.get
    myQueryQueue.foreach { q =>
      q.offer(Some(state)).unsafeRunAndForget()
    }
  }

  override def dispose(): Unit = {
    myQueryQueue.foreach(_.offer(None).unsafeRunSync())
  }

  def getComponent: JComponent = myView

  private def createQueryPipeline(): Unit = {
    Stream
      .eval((Queue.unbounded[IO, Option[QueryContext[Criteria]]], SignallingRef.of[IO, Boolean](false)).parTupled)
      .flatMap { case (queue, initSignal) =>
        Stream
          .resource(Resource.make(IO.delay { myQueryQueue = Some(queue) })(_ => IO.delay { myQueryQueue = None }))
          .flatMap { _ =>
            Stream
              .fromQueueNoneTerminated(queue)
              .evalMapAccumulate(initSignal) { case (lastSignal, context) =>
                lastSignal.set(true) *>
                  SignallingRef
                    .of[IO, Boolean](false)
                    .map((_, context))
              }
              .debounce(200.millis)
              .evalTap((_, context) => updateQueryUI(context))
              .evalTap { (signal, context) =>
                Stream
                  .eval(executeQuery(context))
                  .map { case (pagination, items) =>
                    myQueryStateManager.update(_.copy(pagination = pagination))
                    myQueryResultTableModel.setItems(util.ArrayList[ResultItem](items.asJavaCollection))
                  }
                  .interruptWhen(signal)
                  .compile
                  .drain
                  .attempt
                  .evalAsBackgroundProgress(myProject, "Querying challenges...")
              }
          }
      }
      .compile
      .drain
      .unsafeRunAndForget()
  }

  private def updateQueryUI(context: QueryContext[Criteria]): IO[Unit] =
    IO.delay {
      val tags = createQueryParametersTags(context, { cb => myQueryStateManager.update(cb) })
      myTagsActionModel.set(tags)
    }

  private class QueryStateManager(initialState: QueryContext[Criteria]) {
    @volatile
    private var myStateRef = initialState

    def get: QueryContext[Criteria] = myStateRef

    def update(f: QueryContext[Criteria] => QueryContext[Criteria]): QueryContext[Criteria] = synchronized {
      myStateRef = f(myStateRef)
      myStateRef
    }
  }
}
