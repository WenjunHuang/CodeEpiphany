package com.wenjunhuang.codeepiphany.codeforces.ui

import java.awt.BorderLayout
import javax.swing.ScrollPaneConstants

import com.intellij.ide.plugins.newui.ListPluginComponent
import com.intellij.openapi.actionSystem.{ActionManager, DataSink, UiDataProvider}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.Disposable
import com.intellij.ui.{Gray, JBColor, SearchTextField, SimpleTextAttributes}
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.concurrency.annotations.RequiresEdt

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup
import com.wenjunhuang.codeepiphany.database.tables.records.CodeforcesProblemsetsRecord
import com.wenjunhuang.codeepiphany.leetcode.model.LeetCodeChallengeListItem
import com.wenjunhuang.codeepiphany.model.Actions.TOOLBAR_PLACE
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.ColorUtils

class KeywordSearchView(private val myProject: Project, private val myPresenter: KeywordSearchViewPresenter)
    extends SimpleToolWindowPanel(true, true)
    with UiDataProvider
    with Disposable {
  private val mySearchTextField = SearchTextField(true)
  private val myChallengesTableModel: CodeForcesChallengeListItemTableModel =
    CodeForcesChallengeListItemTableModel(myPresenter)
  private val myChallengesTable =
    myChallengesTableModel.createTableView()

  mySearchTextField.getTextEditor.getEmptyText
    .appendText(
      PluginBundle.message("hackerrank.ui.query.searchHint"),
      new SimpleTextAttributes(
        SimpleTextAttributes.STYLE_PLAIN,
        ColorUtils.LABEL_GRAY_COLOR
      )
    )
  mySearchTextField.addDocumentListener(myPresenter)

  add(mySearchTextField, BorderLayout.NORTH)
  add(
    JBScrollPane(
      myChallengesTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ),
    BorderLayout.CENTER
  )

  private val actionManager           = ActionManager.getInstance()
  private val myQueryRangeActionGroup = PaginationParameterActionGroup()
  private val myQueryRangeToolbar     = actionManager.createActionToolbar(TOOLBAR_PLACE, myQueryRangeActionGroup, true)
  myQueryRangeToolbar.setTargetComponent(this)
  add(myQueryRangeToolbar.getComponent, BorderLayout.SOUTH)

  def getTable: TableView[CodeforcesProblemsetsRecord]     = myChallengesTable
  def getTableModel: CodeForcesChallengeListItemTableModel = myChallengesTableModel

  override def uiDataSnapshot(sink: DataSink): Unit = {
    myPresenter.uiDataSnapshot(sink)
  }

  override def dispose(): Unit = {}

  @RequiresEdt
  def refreshPagination(): Unit =
    myQueryRangeToolbar.updateActionsAsync()
}
