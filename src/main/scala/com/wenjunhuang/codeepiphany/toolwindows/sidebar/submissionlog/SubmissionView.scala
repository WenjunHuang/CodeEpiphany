package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{SimpleToolWindowPanel, Splitter}
import com.intellij.ui.components.{JBLabel, JBScrollPane}
import com.intellij.util.ui.components.BorderLayoutPanel
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType.*
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionView.EMPTY_FORM
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.atcoder.AtCoderSubmissionResultForm
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.codeforces.CodeForcesSubmissionResultForm
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.hackerrank.HackerRankSubmissionResultForm
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.leetcode.LeetCodeSubmissionResultForm
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.luogu.LuoGuSubmissionResultForm

import javax.swing.{JComponent, ScrollPaneConstants, SwingConstants}
import scala.jdk.CollectionConverters.*

class SubmissionView(private val myProject: Project, submissionLogComponent: JComponent)
    extends SimpleToolWindowPanel(true, true) {
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
          myProject,
          language,
          languageVersion,
          challengeSlug,
          CodeDojo.LeetCode,
          submission,
          leetCodeSubmission
        ).getComponent
      case LeetCodeCNSubmission(language, languageVersion, challengeSlug, submission, leetCodeSubmission) =>
        LeetCodeSubmissionResultForm(
          myProject,
          language,
          languageVersion,
          challengeSlug,
          CodeDojo.LeetCodeCN,
          submission,
          leetCodeSubmission
        ).getComponent
      case HackerRankSubmission(language, languageVersion, challengeSlug, contestSlug, submission, hackerCases) =>
        HackerRankSubmissionResultForm(
          myProject,
          language,
          languageVersion,
          challengeSlug,
          contestSlug,
          submission,
          hackerCases.asJavaCollection
        ).getComponent
      case CodeForcesSubmission(language, languageVersion, submission, contestId, problemSetName) =>
        CodeForcesSubmissionResultForm(
          myProject,
          language,
          languageVersion,
          submission,
          contestId,
          problemSetName
        ).getComponent
      case AtCoderSubmission(language, languageVersion, submission, contestId, problemId) =>
        AtCoderSubmissionResultForm(myProject, language, languageVersion, submission, contestId, problemId).getComponent
      case LuoGuSubmission(language, languageVersion, submission) =>
        LuoGuSubmissionResultForm(myProject, language, languageVersion, submission).getComponent
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
    new JBLabel(PluginBundle.message("submission.view.empty"), SwingConstants.CENTER)
  )
}
