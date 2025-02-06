package com.wenjunhuang.codeepiphany.services

import java.awt.BorderLayout
import javax.swing.{ JPanel, ScrollPaneConstants }

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.ui.PopupHandler
import com.intellij.util.ui.EDT
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup
import com.wenjunhuang.codeepiphany.model.Actions.*
import com.wenjunhuang.codeepiphany.utils.ui.TagPane

class ParametersQueryView[Item](private val myPresenter: ParametersQueryPresenter[?, ?, Item])
    extends SimpleToolWindowPanel(true, true)
    with UiDataProvider
    with Disposable {
  private val myParametersToolbar =
    ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, myPresenter.getParametersActionGroup, true)
  myParametersToolbar.setTargetComponent(this)
  setToolbar(myParametersToolbar.getComponent)

  private val myTagPane = TagPane(true, myPresenter.getTagsActionModel)

  Disposer.register(myPresenter, this)

  private val myContent = BorderLayoutPanel()
  myContent.addToTop(myTagPane)

  private val myTable = createTableView()

  myContent.addToCenter(
    JBScrollPane(
      myTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
  )

  private val myQueryRangeActionGroup = PaginationParameterActionGroup()
  private val myQueryRangeToolbar =
    ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, myQueryRangeActionGroup, true)
  myQueryRangeToolbar.setTargetComponent(this)
  myContent.add(myQueryRangeToolbar.getComponent, BorderLayout.SOUTH)
  setContent(myContent)

  def refreshPagination(): Unit = {
    if EDT.isCurrentThreadEdt then myQueryRangeToolbar.updateActionsAsync()
    else ApplicationManager.getApplication.invokeLater(() => myQueryRangeToolbar.updateActionsAsync())
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    myPresenter.uiDataSnapshot(dataSink)

  override def dispose(): Unit = {}

  private def createTableView(): TableView[Item] = {
    val tableView = new TableView[Item](myPresenter.getQueryResultTableModel)
    tableView.setSelectionModel(myPresenter.getQueryResultTableSelectionModel)
    tableView.setShowGrid(false)
    tableView.setShowColumns(true)

    PopupHandler.installRowSelectionTablePopup(
      tableView,
      ActionManager.getInstance().getAction(CHALLENGES_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup],
      CHALLENGES_TABLE_POPUP_PLACE
    )
    tableView
  }

  override def addNotify(): Unit = {
    super.addNotify()
    myPresenter.requery()
  }
}
