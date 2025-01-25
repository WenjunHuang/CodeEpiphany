package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import javax.swing.ScrollPaneConstants

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink}
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.model.Actions
import com.wenjunhuang.codeepiphany.utils.ui.TagPane

class SubmissionLogView(private val myPresenter: SubmissionLogPresenter) extends SimpleToolWindowPanel(true, true) {

  private val myToolbar =
    ActionManager
      .getInstance()
      .createActionToolbar(
        Actions.SUBMISSIONS_TABLE_POPUP_PLACE,
        ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TOOLBAR_GROUP).asInstanceOf[ActionGroup],
        true
      )
  myToolbar.setTargetComponent(this)
  setToolbar(myToolbar.getComponent)

  private val myTableModel = new SubmissionLogTableModel(myPresenter)
  private val myTable      = myTableModel.createTableView(myPresenter.uiDataSnapshot)

  PopupHandler.installRowSelectionTablePopup(
    myTable,
    ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup],
    Actions.SUBMISSIONS_TABLE_POPUP_PLACE
  )

  private val myContent = BorderLayoutPanel()
  myContent.addToCenter(
    JBScrollPane(
      myTable,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
  )

  private val myFilterTagsPane = TagPane()
  myContent.addToTop(myFilterTagsPane)

  setContent(myContent)

  def getTagPane: TagPane                     = myFilterTagsPane
  def getTableModel: SubmissionLogTableModel  = myTableModel
  def getTable: TableView[SubmissionLogEntry] = myTable

  override def addNotify(): Unit = {
    super.addNotify()
    myPresenter.requery()
  }

  override def uiDataSnapshot(sink: DataSink): Unit = {
    myPresenter.uiDataSnapshot(sink)
  }
}
