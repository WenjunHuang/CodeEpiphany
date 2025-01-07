package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ ToolWindow, ToolWindowFactory }
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCodeCN

import scala.annotation.static

class SidebarWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    FileEditorManager.getInstance(project).getFocusedEditor
    val presenter = ChallengeDescriptionPresenter(project)
    val cm        = toolWindow.getContentManager
    val cf        = cm.getFactory

    cm.addContent(cf.createContent(presenter.getView, "Challenge Description", false))
    cm.addContent(cf.createContent(LogConsoleView(project), LogConsoleView.DISPLAY_NAME, false))
    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
  }
}

object SidebarWindowFactory {
  @static
  final val TOOL_WINDOW_ID = "CodeEpiphany.Sidebar"
}
