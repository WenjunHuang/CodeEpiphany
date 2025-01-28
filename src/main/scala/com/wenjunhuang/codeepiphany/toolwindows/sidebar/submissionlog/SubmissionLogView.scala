package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import javax.swing.{ JComponent, ScrollPaneConstants, SwingConstants }

import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink }
import com.intellij.openapi.ui.{ SimpleToolWindowPanel, Splitter }
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.{ JBLabel, JBScrollPane }
import com.intellij.ui.table.TableView
import com.intellij.util.ui.components.BorderLayoutPanel
import scala.jdk.CollectionConverters.*

import com.wenjunhuang.codeepiphany.model.Actions
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType.{
  HackerRankSubmission,
  LeetCodeCNSubmission,
  LeetCodeSubmission
}
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogView.EMPTY_FORM
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.leetcode.LeetCodeSubmissionResultForm
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.hackerrank.HackerRankSubmissionResultForm
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
  private val myTable      = myTableModel.createTableView()

  PopupHandler.installRowSelectionTablePopup(
    myTable,
    ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup],
    Actions.SUBMISSIONS_TABLE_POPUP_PLACE
  )

  private val myFilterTagsPane = TagPane()

  private val mySplitter: Splitter = initSplitter()

  private def initSplitter(): Splitter = {
    val mySubmissionLogPane = BorderLayoutPanel()
      .addToCenter(
        JBScrollPane(
          myTable,
          ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
      )
      .addToTop(myFilterTagsPane)

    val mySplitter = Splitter()
    mySplitter.setShowDividerControls(true)
    mySplitter.setFirstComponent(mySubmissionLogPane)
    mySplitter.setSecondComponent(EMPTY_FORM)

    setContent(mySplitter)
    mySplitter
  }

  def setDetailEmpty(): Unit = {
    mySplitter.setSecondComponent(EMPTY_FORM)
  }

  def setDetail(submissionType: SubmissionType): Unit = {
    val comp = submissionType match {
      case LeetCodeSubmission(language, submission, leetCodeSubmission) =>
        LeetCodeSubmissionResultForm(language, submission, leetCodeSubmission).getComponent
      case LeetCodeCNSubmission(language, submission, leetCodeSubmission) =>
        LeetCodeSubmissionResultForm(language, submission, leetCodeSubmission).getComponent
      case HackerRankSubmission(language, submission, hackerCases) =>
        HackerRankSubmissionResultForm(language, submission, hackerCases.asJavaCollection).getComponent
    }
    mySplitter.setSecondComponent(
      JBScrollPane(
        comp,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      )
    )
  }

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

object SubmissionLogView {
  val EMPTY_FORM: JComponent = BorderLayoutPanel().addToCenter(
    new JBLabel("Please select a submission to view the result.", SwingConstants.CENTER)
  )
}
