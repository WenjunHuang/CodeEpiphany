package com.wenjunhuang.codeepiphany.services

import java.awt.BorderLayout
import javax.swing.ScrollPaneConstants

import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.{ PopupHandler, SearchTextField, SimpleTextAttributes }
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.EDT

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup
import com.wenjunhuang.codeepiphany.hackerrank.model.HackerRankChallengeDetail
import com.wenjunhuang.codeepiphany.model.Actions.{
  CHALLENGES_TABLE_POPUP_GROUP,
  CHALLENGES_TABLE_POPUP_PLACE,
  TOOLBAR_PLACE
}
import com.wenjunhuang.codeepiphany.utils.ColorUtils

class KeywordQueryView[Item](private val myPresenter: KeywordQueryPresenter[?, ?, Item])
    extends SimpleToolWindowPanel(true, true)
    with UiDataProvider {

  private val mySearchTextField       = SearchTextField(true)
  private val myTable                 = createTableView()
  private val myContent               = BorderLayoutPanel()
  private val myQueryRangeActionGroup = PaginationParameterActionGroup()
  private val myQueryRangeToolbar =
    ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, myQueryRangeActionGroup, true)

  mySearchTextField.getTextEditor.getEmptyText
    .appendText(
      PluginBundle.message("ui.query.searchHint"),
      new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, ColorUtils.LABEL_GRAY_COLOR)
    )
  mySearchTextField.addDocumentListener(myPresenter.getDocumentAdapter)
  add(mySearchTextField, BorderLayout.NORTH)

  myContent.addToCenter(
    JBScrollPane(
      myTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
  )
  myQueryRangeToolbar.setTargetComponent(this)
  myContent.addToBottom(myQueryRangeToolbar.getComponent)

  setContent(myContent)

  override def uiDataSnapshot(sink: DataSink): Unit = {
    myPresenter.uiDataSnapshot(sink)
  }

  def refreshPagination(): Unit = {
    if EDT.isCurrentThreadEdt then myQueryRangeToolbar.updateActionsAsync()
    else ApplicationManager.getApplication.invokeLater(() => myQueryRangeToolbar.updateActionsAsync())
  }

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
