package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.wenjunhuang.codeepiphany.hackerrank.model
import com.wenjunhuang.codeepiphany.toolwindows.dojo.AbstractCodeDojoView
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.PaginationActionGroup
import com.wenjunhuang.codeepiphany.model.Actions.*
import com.wenjunhuang.codeepiphany.utils.ui.TagPane

import java.awt.BorderLayout
import javax.swing.{JPanel, ScrollPaneConstants}

class QueryParamView(private val myProject: Project, private val myPresenter: QueryParametersViewPresenter)
    extends SimpleToolWindowPanel(true,true)
    with AbstractCodeDojoView {
  private val actionManager = ActionManager.getInstance()
  private val myActionGroup = actionManager.getAction(HACKERRANK_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  private val myMainToolbar = actionManager.createActionToolbar(TOOLBAR_PLACE, myActionGroup, true)
  myMainToolbar.setTargetComponent(this)
  setToolbar(myMainToolbar.getComponent)

  private val myTagPane = TagPane()
//  private val myTagActionGroup = DefaultActionGroup()
//  private val myTagToolbar     = actionManager.createActionToolbar(TOOLBAR_PLACE, myTagActionGroup, true)
//  myTagToolbar.setTargetComponent(this)
//  myTagToolbar.setLayoutStrategy(ToolbarLayoutStrategy.WRAP_STRATEGY)
//  myTagToolbar.setReservePlaceAutoPopupIcon(false)
//
  Disposer.register(myPresenter, this)

  private val myContent = JPanel(BorderLayout())
  myContent.add(myTagPane, BorderLayout.NORTH)

  private val myChallengesTableModel: ChallengesTableModel = ChallengesTableModel()
  private val myChallengesTable: TableView[model.ChallengeDetail] =
    myChallengesTableModel.createTableView(uiDataSnapshot)

  myContent.add(
    JBScrollPane(
      myChallengesTable,
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

  def getTableModel: ChallengesTableModel   = myChallengesTableModel
  def getTable: TableView[model.ChallengeDetail] = myChallengesTable
  def getTagPane: TagPane = myTagPane

  def refreshTagToolbar(): Unit =
    ApplicationManager.getApplication.invokeLater(() => myTagPane.updateActionsAsync())

  def refreshPagination(): Unit =
    ApplicationManager.getApplication.invokeLater(() => myQueryRangeToolbar.updateActionsAsync())

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    myPresenter.uiDataSnapshot(dataSink)

  override def dispose(): Unit = {}

}
