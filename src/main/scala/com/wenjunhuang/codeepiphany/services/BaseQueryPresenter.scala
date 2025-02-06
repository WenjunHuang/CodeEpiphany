package com.wenjunhuang.codeepiphany.services

import cats.effect.{IO, Resource}
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import java.util
import javax.swing.{JComponent, ListSelectionModel}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SingleSelectionModel
import com.intellij.util.ui.{ColumnInfo, ListTableModel}

import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup.{PAGINATION_PROVIDER_KEY, PaginationParameterProvider}
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, PageSize, Pagination}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

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
  protected val myQueryStateManager = QueryStateManager(createInitialQueryParameters(myBoostrapParameters))
  protected val myQueryResultTableModel: ListTableModel[ResultItem] = ListTableModel[ResultItem]()
  myQueryResultTableModel.setColumnInfos(getQueryResultColumns.asInstanceOf[Array[ColumnInfo[?, ?]]])
  protected val myQueryResultSelectionModel: SingleSelectionModel = SingleSelectionModel()

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

  protected def createInitialQueryParameters(boostrapParameters: UIBoostrapParameters): QueryContext[T]
  protected def executeQuery(context: QueryContext[T]): IO[(Pagination, List[ResultItem])]
  protected def refreshPagination(): Unit
  protected def updateQueryUI(context: QueryContext[T]): Unit

  def getQueryResultColumns: Array[OrderByColumnInfo[ResultItem, ?]]

  def requery(): Unit = {
    val state = myQueryStateManager.get
    myQueryQueue.foreach { q =>
      q.offer(Some(state)).unsafeRunAndForget()
    }
  }

  override def dispose(): Unit = {
    myQueryQueue.foreach(_.offer(None).unsafeRunSync())
  }

  private def createQueryPipeline(): Unit = {
    Stream
      .eval((Queue.unbounded[IO, Option[QueryContext[T]]], SignallingRef.of[IO, Boolean](false)).parTupled)
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
              .debounce(300.millis)
              .evalTap((_, context) => IO.delay { updateQueryUI(context) }.evalOnEDTAny())
              .evalTap { (signal, context) =>
                Stream
                  .eval(executeQuery(context))
                  .evalTap { case (pagination, items) =>
                    IO.delay {
                      myQueryStateManager.update(_.copy(pagination = pagination))
                      myQueryResultTableModel.setItems(util.ArrayList[ResultItem](items.asJavaCollection))
                      refreshPagination()
                    }.evalOnEDTAny()
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

  private val myPaginationProvider = new PaginationParameterProvider {
    private val allItems = List(PageSize.Twenty, PageSize.Fifty, PageSize.OneHundred)

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
