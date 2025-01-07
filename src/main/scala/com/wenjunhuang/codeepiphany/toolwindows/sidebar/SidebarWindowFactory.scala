package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory, ToolWindowManager}
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.description.ChallengeDescriptionPresenter

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

  def activate(project: Project, displayName: String): Unit = {
    Option(ToolWindowManager.getInstance(project).getToolWindow(SidebarWindowFactory.TOOL_WINDOW_ID)) match
      case None => {}
      case Some(twm) =>
        twm.show()
//        twm.activate(() => {
//          val cm = twm.getContentManager
//          Option(cm.findContent(displayName)).foreach(cm.setSelectedContent)
//        })
  }
}
