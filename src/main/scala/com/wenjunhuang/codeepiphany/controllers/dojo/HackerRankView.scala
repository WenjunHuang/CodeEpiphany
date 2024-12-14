package com.wenjunhuang.codeepiphany.controllers.dojo

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.{ JBLabel, JBScrollPane }
import com.intellij.ui.table.JBTable
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.groups.*

import javax.swing.{ JScrollPane, ScrollPaneConstants, SwingConstants }
import javax.swing.table.{ DefaultTableModel, JTableHeader }

class HackerRankView(private val myProject: Project, private val myPresenter: HackerRankPresenter) extends SimpleToolWindowPanel(true, true) with AbstractCodeDojoViewPanel {
  private val actionManager = ActionManager.getInstance()
  private val myActionGroup = actionManager.getAction(HACKERRANK_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  private val myMainToolbar = actionManager.createActionToolbar(TOOLBAR_PLACE, myActionGroup, true)
  myMainToolbar.setTargetComponent(this)
  setToolbar(myMainToolbar.getComponent)

  private val myTagActionGroup = DefaultActionGroup()
  private val myTagToolbar     = actionManager.createActionToolbar(TOOLBAR_PLACE, myTagActionGroup, true)
  myTagToolbar.setTargetComponent(this)
  myTagToolbar.setLayoutStrategy(ToolbarLayoutStrategy.WRAP_STRATEGY)
  myTagToolbar.setReservePlaceAutoPopupIcon(false)

  Disposer.register(myPresenter, this)

  private val myContent = SimpleToolWindowPanel(true, true)
  myContent.setToolbar(myTagToolbar.getComponent)

  private val myQuestionsModel = DefaultTableModel(Array(Array[Any]("Solved", "Two Sum", "Easy", "Array, Hash Table")), Array[Any]("State", "Title", "Difficulty", "Tags"))
  private val myQuestionsTable = JBTable(myQuestionsModel)
  myQuestionsTable.setShowGrid(false)
  myQuestionsTable.setShowColumns(true)
  myContent.setContent(
    JBScrollPane(
      myQuestionsTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
  )
  setContent(myContent)

  def getTagActionGroup: DefaultActionGroup = myTagActionGroup
  def refreshTagToolbar(): Unit             = myTagToolbar.updateActionsAsync()

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    myPresenter.uiDataSnapshot(dataSink)

  override def dispose(): Unit = {}

}
