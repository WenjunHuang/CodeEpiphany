package com.wenjunhuang.codeepiphany.controllers.dojo.hackerrank

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.wenjunhuang.codeepiphany.controllers.dojo.AbstractCodeDojoView
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.PaginationActionGroup
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.groups.*
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeDetail

import java.awt.BorderLayout
import javax.swing.{JPanel, ScrollPaneConstants}
import scala.jdk.CollectionConverters.*

class QueryParamView(private val myProject: Project, private val myPresenter: QueryParametersViewPresenter) extends SimpleToolWindowPanel(true, true) with AbstractCodeDojoView {
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

  private val myContent = JPanel(BorderLayout())
  myContent.add(myTagToolbar.getComponent, BorderLayout.NORTH)

  private val myQuestionsModel = ChallengesTableModel()
  private val myQuestionsTable = TableView(myQuestionsModel)
  myQuestionsTable.setShowGrid(false)
  myQuestionsTable.setShowColumns(true)
  
  myContent.add(
    JBScrollPane(
      myQuestionsTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ),
    BorderLayout.CENTER
  )

  private val myQueryRangeActionGroup = PaginationActionGroup()
  private val myQueryRangeToolbar     = actionManager.createActionToolbar(TOOLBAR_PLACE, myQueryRangeActionGroup, true)
  myQueryRangeToolbar.setTargetComponent(this)
  myContent.add(myQueryRangeToolbar.getComponent, BorderLayout.SOUTH)
  setContent(myContent)
  
  def getTagActionGroup: DefaultActionGroup = myTagActionGroup

  def refreshTagToolbar(): Unit =
    ApplicationManager.getApplication.invokeLater(() => myTagToolbar.updateActionsAsync())

  def refreshPagination(): Unit =
    ApplicationManager.getApplication.invokeLater(() => myQueryRangeToolbar.updateActionsAsync())

  def setChallengeItems(items: List[ChallengeDetail]): Unit =
    myQuestionsModel.setItems(items.asJava)

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    myPresenter.uiDataSnapshot(dataSink)

  override def dispose(): Unit = {}

}
