package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import javax.swing.{ JComponent, ScrollPaneConstants, SwingConstants }
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import com.intellij.openapi.ui.{ SimpleToolWindowPanel, Splitter }
import com.intellij.ui.components.{ JBLabel, JBScrollPane }
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType.*
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionView.EMPTY_FORM
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.atcoder.AtCoderSubmissionResultForm
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
      case LeetCodeSubmission(language, languageVersion, challengeSlug, submission, leetCodeSubmission) =>
        LeetCodeSubmissionResultForm(
          language,
          languageVersion,
          challengeSlug,
          CodeDojo.LeetCode,
          submission,
          leetCodeSubmission
        ).getComponent
      case LeetCodeCNSubmission(language, languageVersion, challengeSlug, submission, leetCodeSubmission) =>
        LeetCodeSubmissionResultForm(
          language,
          languageVersion,
          challengeSlug,
          CodeDojo.LeetCodeCN,
          submission,
          leetCodeSubmission
        ).getComponent
      case HackerRankSubmission(language, languageVersion, challengeSlug,contestSlug, submission, hackerCases) =>
        HackerRankSubmissionResultForm(
          language,
          languageVersion,
          challengeSlug,
          contestSlug.toJava,
          submission,
          hackerCases.asJavaCollection
        ).getComponent
      case CodeForcesSubmission(language, languageVersion, submission, contestId, problemsetName) =>
        CodeForcesSubmissionResultForm(
          language,
          languageVersion,
          submission,
          contestId,
          problemsetName.toJava
        ).getComponent
      case AtCoderSubmission(language, languageVersion, submission, contestId, problemId) =>
        AtCoderSubmissionResultForm(language, languageVersion, submission, contestId, problemId).getComponent
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
