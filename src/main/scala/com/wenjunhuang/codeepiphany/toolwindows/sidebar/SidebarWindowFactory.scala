package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.{ ToolWindow, ToolWindowManager }
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi

import com.wenjunhuang.codeepiphany.toolwindows.sidebar.description.ChallengeDescriptionPresenter
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.notes.NotesListPresenter
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionPresenter
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.SidebarWindowFactory.*
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.SolutionsPresenter
import com.wenjunhuang.codeepiphany.utils.walkaround.ToolWindowFactoryBridge

class SidebarWindowFactory extends ToolWindowFactoryBridge {
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val cm = toolWindow.getContentManager
    val cf = cm.getFactory

    val descriptionPresenter = ChallengeDescriptionPresenter(project)
    val submissionPresenter  = SubmissionPresenter(project)
    val notesPresenter       = NotesListPresenter(project)
    val solutionsPresenter   = SolutionsPresenter(project)

    cm.addContent(cf.createContent(LogConsoleView(project), CONSOLE, false))
    cm.addContent(cf.createContent(descriptionPresenter.getView, DESCRIPTION, false))
    cm.addContent(cf.createContent(submissionPresenter.getView, SUBMISSIONS, false))
    cm.addContent(cf.createContent(solutionsPresenter.getView, SOLUTIONS, false))
    cm.addContent(cf.createContent(notesPresenter.getView, NOTES, false))

    Disposer.register(project, descriptionPresenter)
    Disposer.register(project, submissionPresenter)
    Disposer.register(project, notesPresenter)

    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
  }
}

object SidebarWindowFactory {
  final val TOOL_WINDOW_ID = "LeetCode Epiphany Sidebar"

  final val CONSOLE     = LogConsoleView.DISPLAY_NAME
  final val DESCRIPTION = "Description"
  final val SUBMISSIONS = "Submissions"
  final val SOLUTIONS   = "Solutions"
  final val NOTES       = "Notes"

  def getToolWindow(project: Project): ToolWindow = {
    ToolWindowManager.getInstance(project).getToolWindow(SidebarWindowFactory.TOOL_WINDOW_ID) match
      case null => throw IllegalStateException(s"Could not find tool window for id ${TOOL_WINDOW_ID}")
      case tw   => tw
  }

  def activate(project: Project, displayName: String): Unit = {
    val twm = getToolWindow(project)
    if !twm.isAvailable then twm.setAvailable(true)
    if !twm.isActive then twm.activate(null)
    twm.getContentManager.findContent(displayName) match
      case null =>
      case content =>
        twm.getContentManager.setSelectedContent(content)
  }
}
