package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink}
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import com.wenjunhuang.codeepiphany.model.Actions
import com.wenjunhuang.codeepiphany.utils.ui.TagPane

import javax.swing.ScrollPaneConstants

class SubmissionLogView(private val myPresenter: SubmissionLogPresenter) extends SimpleToolWindowPanel(true, true) {
  private val myActionGroup =
    ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  private val myTagPane    = TagPane()
  private val myTableModel = new SubmissionLogTableModel()
  private val myTable      = myTableModel.createTableView(myPresenter.uiDataSnapshot)
  private val myContent    = BorderLayoutPanel()
  private val myMainToolbar =
    ActionManager.getInstance().createActionToolbar(Actions.SUBMISSIONS_TABLE_POPUP_PLACE, myActionGroup, true)
  myMainToolbar.setTargetComponent(this)
  setToolbar(myMainToolbar.getComponent)

  myContent.addToCenter(
    JBScrollPane(
      myTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
  )
  myContent.addToTop(myTagPane)

  setContent(myContent)

  def getTagPane: TagPane = myTagPane

  override def uiDataSnapshot(sink: DataSink): Unit = {
    myPresenter.uiDataSnapshot(sink)
  }
}
