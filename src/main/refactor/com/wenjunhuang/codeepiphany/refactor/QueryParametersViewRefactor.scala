package com.wenjunhuang.codeepiphany.refactor

import java.awt.BorderLayout
import javax.swing.{ JPanel, ListSelectionModel, ScrollPaneConstants }
import javax.swing.table.TableModel

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.{ BaseTableView, TableView }
import com.intellij.ui.PopupHandler
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.ListTableModel

import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup
import com.wenjunhuang.codeepiphany.hackerrank.model
import com.wenjunhuang.codeepiphany.hackerrank.model.HackerRankChallengeDetail
import com.wenjunhuang.codeepiphany.model.Actions.*
import com.wenjunhuang.codeepiphany.utils.ui.TagPane

class QueryParametersViewRefactor[Item](private val myPresenter: QueryParametersPresenterRefactor[?, ?, Item])
    extends SimpleToolWindowPanel(true, true)
    with UiDataProvider
    with Disposable {
  private val myParametersToolbar =
    ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, myPresenter.getParametersActionGroup, true)
  myParametersToolbar.setTargetComponent(this)
  setToolbar(myParametersToolbar.getComponent)

  private val myTagPane = TagPane(true,myPresenter.getTagsActionModel)

  Disposer.register(myPresenter, this)

  private val myContent = JPanel(BorderLayout())
  myContent.add(myTagPane, BorderLayout.NORTH)

  private val myTable = createTableView()

  myContent.add(
    JBScrollPane(
      myTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ),
    BorderLayout.CENTER
  )

  private val myQueryRangeActionGroup = PaginationParameterActionGroup()
  private val myQueryRangeToolbar =
    ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, myQueryRangeActionGroup, true)
  myQueryRangeToolbar.setTargetComponent(this)
  myContent.add(myQueryRangeToolbar.getComponent, BorderLayout.SOUTH)
  setContent(myContent)

  @RequiresEdt
  def refreshPagination(): Unit =
    myQueryRangeToolbar.updateActionsAsync()

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
}
