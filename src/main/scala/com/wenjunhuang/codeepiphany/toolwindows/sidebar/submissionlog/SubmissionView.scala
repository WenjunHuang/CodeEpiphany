package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import javax.swing.{JComponent, ScrollPaneConstants, SwingConstants}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import com.intellij.openapi.ui.{SimpleToolWindowPanel, Splitter}
import com.intellij.ui.components.{JBLabel, JBScrollPane}
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType.{CodeForcesSubmission, HackerRankSubmission, LeetCodeCNSubmission, LeetCodeSubmission}
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionView.EMPTY_FORM
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.codeforces.CodeForcesSubmissionResultForm
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.hackerrank.HackerRankSubmissionResultForm
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.leetcode.LeetCodeSubmissionResultForm

class SubmissionView(submissionLogComponent: JComponent) extends SimpleToolWindowPanel(true, true) {
  private val mySplitter: Splitter = initSplitter()

  private def initSplitter(): Splitter = {
    val mySplitter = Splitter()
    mySplitter.setShowDividerControls(true)
    mySplitter.setFirstComponent(submissionLogComponent)
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
      case CodeForcesSubmission(language, submission, contestId, problemsetName) =>
        CodeForcesSubmissionResultForm(language, submission, contestId, problemsetName.toJava).getComponent
    }
    mySplitter.setSecondComponent(
      JBScrollPane(
        comp,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      )
    )
  }
}

object SubmissionView {
  val EMPTY_FORM: JComponent = BorderLayoutPanel().addToCenter(
    new JBLabel("Please select a submission to view the result.", SwingConstants.CENTER)
  )
}
