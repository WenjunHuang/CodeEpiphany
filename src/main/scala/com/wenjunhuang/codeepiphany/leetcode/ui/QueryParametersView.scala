package com.wenjunhuang.codeepiphany.leetcode.ui

import java.awt.BorderLayout
import javax.swing.{JPanel, ScrollPaneConstants}

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.concurrency.annotations.RequiresEdt

import com.wenjunhuang.codeepiphany.actions.PaginationParameterActionGroup
import com.wenjunhuang.codeepiphany.leetcode.model.LeetCodeChallengeListItem
import com.wenjunhuang.codeepiphany.model.Actions.*
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.ui.TagPane

class QueryParametersView(
  private val myProject: Project,
  private val myPresenter: QueryParametersPresenter,
  private val myCodeDojo: CodeDojo
) extends SimpleToolWindowPanel(true, true)
    with UiDataProvider
    with Disposable {
  private val actionManager = ActionManager.getInstance()
  private val myActionGroup = actionManager.getAction(LEETCODE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  private val myMainToolbar = actionManager.createActionToolbar(TOOLBAR_PLACE, myActionGroup, true)
  myMainToolbar.setTargetComponent(this)
  setToolbar(myMainToolbar.getComponent)

  private val myTagPane = TagPane()

  Disposer.register(myPresenter, this)

  private val myContent = JPanel(BorderLayout())
  myContent.add(myTagPane, BorderLayout.NORTH)

  private val myChallengesTableModel = LeetCodeChallengeListItemTableModel(myPresenter, myCodeDojo)
  private val myChallengesTable      = myChallengesTableModel.createTableView(uiDataSnapshot)

  myContent.add(
    JBScrollPane(
      myChallengesTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ),
    BorderLayout.CENTER
  )

  private val myQueryRangeActionGroup = PaginationParameterActionGroup()
  private val myQueryRangeToolbar     = actionManager.createActionToolbar(TOOLBAR_PLACE, myQueryRangeActionGroup, true)
  myQueryRangeToolbar.setTargetComponent(this)
  myContent.add(myQueryRangeToolbar.getComponent, BorderLayout.SOUTH)
  setContent(myContent)

  def getTableModel: LeetCodeChallengeListItemTableModel = myChallengesTableModel
  def getTable: TableView[LeetCodeChallengeListItem]     = myChallengesTable
  def getTagPane: TagPane                                = myTagPane

  @RequiresEdt
  def refreshTagToolbar(): Unit =
    myTagPane.updateActionsAsync()

  @RequiresEdt
  def refreshPagination(): Unit =
    myQueryRangeToolbar.updateActionsAsync()

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    myPresenter.uiDataSnapshot(dataSink)

  override def dispose(): Unit = {}

}
