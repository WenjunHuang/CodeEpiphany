package com.wenjunhuang.codeepiphany.services

import cats.effect.std.Queue
import cats.effect.{ IO, Resource, SyncIO }
import cats.syntax.all.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SingleSelectionModel
import com.intellij.util.ui.{ ColumnInfo, ListTableModel }
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.{
  PAGINATION_PROVIDER_KEY,
  PaginationParameterProvider
}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.{ CancellableStream, OrderByColumnInfo, PageSize, Pagination }
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.typelevel.log4cats.{ Logger, LoggerFactory }

import java.util
import javax.swing.{ JComponent, ListSelectionModel }
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

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

class QueryStateManager[T](initialState: QueryContext[T]) {
  @volatile
  private var myStateRef = initialState

  def get: QueryContext[T] = myStateRef

  def update(f: QueryContext[T] => QueryContext[T]): QueryContext[T] = synchronized {
    myStateRef = f(myStateRef)
    myStateRef
  }
}

abstract class BaseQueryPresenter[UIBoostrapParameters, T, ResultItem](
  protected val myProject: Project,
  protected val myBoostrapParameters: UIBoostrapParameters
) extends Disposable {
  protected val myLoggerIO: Logger[IO]   = LoggerFactory.getLogger[IO]
  protected val myLogger: Logger[SyncIO] = LoggerFactory.getLogger[SyncIO]
  protected val myQueryStateManager = QueryStateManager(
    loadSavedCriteriaOrCreateFromBootstrapParameters(myBoostrapParameters)
  )
  protected val myQueryResultTableModel: ListTableModel[ResultItem] = ListTableModel[ResultItem]()
  myQueryResultTableModel.setColumnInfos(getQueryResultColumns.asInstanceOf[Array[ColumnInfo[?, ?]]])
  protected val myQueryResultSelectionModel: ListSelectionModel = createQueryResultSelectionModel()

  @volatile
  private var myIsQuerying = false

  @volatile
  private var myQueryQueue: Option[Queue[IO, Option[QueryContext[T]]]] = None
  createQueryPipeline()
  Disposer.register(myProject, this)

  def getViewComponent: JComponent
  def getQueryResultTableModel: ListTableModel[ResultItem]  = myQueryResultTableModel
  def getQueryResultTableSelectionModel: ListSelectionModel = myQueryResultSelectionModel

  def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(PAGINATION_PROVIDER_KEY, myPaginationProvider)
  }

  protected def saveQueryCriteria(queryCriteria: T, pagination: Pagination): Unit = {}
  protected def loadQueryCriteria(): Option[(T, Pagination)]                      = None

  protected def createQueryResultSelectionModel(): ListSelectionModel = SingleSelectionModel()
  protected def createInitialQueryParameters(boostrapParameters: UIBoostrapParameters): QueryContext[T]
  protected def executeQuery(context: QueryContext[T]): IO[(Pagination, List[ResultItem])]
  protected def refreshPagination(): Unit
  protected def updateQueryUI(context: QueryContext[T]): Unit
  protected def queryTitle: String = ""

  private def loadSavedCriteriaOrCreateFromBootstrapParameters(
    boostrapParameters: UIBoostrapParameters
  ): QueryContext[T] = {
    loadQueryCriteria() match
      case None                   => createInitialQueryParameters(boostrapParameters)
      case Some((criteria, page)) => QueryContext[T](criteria, page)

  }
  def getQueryResultColumns: Array[OrderByColumnInfo[ResultItem, ?]]

  def requery(resetToFirstPage: Boolean = false): Unit = {
    val state = if resetToFirstPage then myQueryStateManager.get.resetToFirstPage else myQueryStateManager.get
    myQueryQueue.foreach { q =>
      q.offer(Some(state)).unsafeRunAndForget()
    }
  }

  def isQuerying: Boolean = myIsQuerying

  override def dispose(): Unit = {
    myQueryQueue.foreach(_.offer(None).unsafeRunSync())
  }

  private def handleQueryResult(pagination: Pagination, items: List[ResultItem]): IO[Unit] = IO.delay {
    val updatedCriteria = myQueryStateManager.update(_.copy(pagination = pagination)).criteria
    myQueryResultTableModel.setItems(util.ArrayList[ResultItem](items.asJavaCollection))
    refreshPagination()
    saveQueryCriteria(updatedCriteria, pagination)
  }.evalOnEDTAny()

  private def executeQueryWithProgress(context: QueryContext[T]): IO[Unit] = {
    Stream
      .eval(IO.delay { myIsQuerying = true } *> executeQuery(context))
      .evalTap { case (pagination, items) => handleQueryResult(pagination, items) }
      .onFinalize(IO.delay { myIsQuerying = false })
      .compile
      .drain
      .recoverWith { e =>
        myLoggerIO.warn(e)("Failed to execute query") *>
          console.error(myProject, PluginBundle.message("query.error", e.getMessage))
      }
      .evalAsBackgroundProgress(myProject, PluginBundle.message("query.progress.title", queryTitle))
  }

  private def processQuery(ctx: CancellableStream.StreamContext[QueryContext[T]]): IO[Unit] = {
    IO.delay { updateQueryUI(ctx.value) }.evalOnEDTAny() *>
      Stream
        .eval(executeQueryWithProgress(ctx.value))
        .interruptWhen(ctx.signal)
        .compile
        .drain
  }

  private def createQueryPipeline(): Unit = {
    CancellableStream
      .setup[QueryContext[T], Unit](300.millis)(processQuery)
      .evalMap(queue => IO.delay { myQueryQueue = Some(queue) })
      .use(_ => IO.never)
      .unsafeRunAndForget()
  }

  protected def pageSizes: List[PageSize] = List(PageSize.Twenty, PageSize.Fifty, PageSize.OneHundred)

  private val myPaginationProvider = new PaginationParameterProvider {
    private val allItems = pageSizes

    override def getAllItems: List[PageSize]  = allItems
    override def refresh(): Unit              = refreshPagination()
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
}
