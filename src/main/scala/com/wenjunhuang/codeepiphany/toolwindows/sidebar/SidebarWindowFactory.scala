package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import scala.annotation.static

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory, ToolWindowManager}
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi

import com.wenjunhuang.codeepiphany.toolwindows.sidebar.description.ChallengeDescriptionPresenter
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solution.SolutionListPresenter
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter

class SidebarWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    FileEditorManager.getInstance(project).getFocusedEditor
    val descriptionPresenter   = ChallengeDescriptionPresenter(project)
    val submissionLogPresenter = SubmissionLogPresenter(project)
    val solutionPresenter      = SolutionListPresenter(project)
    val cm                     = toolWindow.getContentManager
    val cf                     = cm.getFactory

    cm.addContent(cf.createContent(LogConsoleView(project), LogConsoleView.DISPLAY_NAME, false))
    cm.addContent(cf.createContent(descriptionPresenter.getView, "Description", false))
    cm.addContent(cf.createContent(submissionLogPresenter.getView, "Submissions", false))
    cm.addContent(cf.createContent(solutionPresenter.getView, "Solutions", false))
    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
  }
}

object SidebarWindowFactory {
  @static
  final val TOOL_WINDOW_ID = "CodeEpiphany Sidebar"

  final val CONSOLE     = LogConsoleView.DISPLAY_NAME
  final val DESCRIPTION = "Description"
  final val SUBMISSIONS = "Submissions"
  final val SOLUTIONS   = "Solutions"

  def activate(project: Project, displayName: String): Unit = {
    Option(ToolWindowManager.getInstance(project).getToolWindow(SidebarWindowFactory.TOOL_WINDOW_ID)) match
      case Some(twm) =>
        if !twm.isAvailable then twm.setAvailable(true)
        if !twm.isActive then twm.activate(null)
        twm.getContentManager.findContent(displayName) match
          case null =>
          case content =>
            twm.getContentManager.setSelectedContent(content)
      case None =>
  }
}
