package com.wenjunhuang.codeepiphany.services

import javax.swing.JComponent

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink, DefaultActionGroup}
import com.intellij.openapi.observable.properties.{AtomicProperty, ObservableProperty}
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.actions.RefreshAction.{REFRESH_PROVIDER_KEY, RefreshProvider}
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

abstract class ParametersQueryPresenter[UIBoostrapParameters, T, ResultItem](
  project: Project,
  boostrapParameters: UIBoostrapParameters
) extends BaseQueryPresenter[UIBoostrapParameters, T, ResultItem](project, boostrapParameters) {

  protected val myQueryParametersActionGroup: DefaultActionGroup       = DefaultActionGroup()
  protected val myTagsActionModel: AtomicProperty[List[TagPaneAction]] = AtomicProperty[List[TagPaneAction]](Nil)
  protected var myView: Option[ParametersQueryView[ResultItem]]        = None

  protected def prepareProviders(
    getter: () => QueryContext[T],
    updater: (QueryContext[T] => QueryContext[T]) => Unit,
    dataSink: DataSink
  ): ActionGroup
  protected def createQueryParametersTags(
    context: QueryContext[T],
    onCloseUpdater: (QueryContext[T] => QueryContext[T]) => Unit
  ): List[TagPaneAction]

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    super.uiDataSnapshot(dataSink)

    val actions =
      prepareProviders(
        { () => myQueryStateManager.get },
        { cb =>
          myQueryStateManager.update(cb)
          requery()
        },
        dataSink
      )
    myQueryParametersActionGroup.removeAll()
    myQueryParametersActionGroup.addAll(actions.getChildren(null, ActionManager.getInstance())*)
    dataSink.set(REFRESH_PROVIDER_KEY, myRefreshProvider)
  }

  def getParametersActionGroup: ActionGroup                       = myQueryParametersActionGroup
  def getTagsActionModel: ObservableProperty[List[TagPaneAction]] = myTagsActionModel

  override protected def refreshPagination(): Unit = {
    myView.foreach(_.refreshPagination())
  }

  override def getViewComponent: JComponent = synchronized {
    myView match {
      case Some(view) => view.getComponent
      case None =>
        val view = ParametersQueryView[ResultItem](this)
        myView = Some(view)
        view.getComponent
    }
  }

  override protected def updateQueryUI(context: QueryContext[T]): Unit = {
    val tags = createQueryParametersTags(
      context,
      { cb =>
        myQueryStateManager.update(cb)
        requery()
      }
    )
    myTagsActionModel.set(tags)
  }

  private val myRefreshProvider = new RefreshProvider {
    override def refresh(): Unit =
      requery()
  }

}
