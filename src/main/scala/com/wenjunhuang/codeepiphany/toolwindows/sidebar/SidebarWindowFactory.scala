package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory, ToolWindowManager}
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.wenjunhuang.codeepiphany.model.Repository.ChallengeStorageItem
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCodeCN
import kotlin.coroutines.Continuation

import scala.annotation.static

class SidebarWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val presenter = ChallengeDescriptionPresenter(project)
    val cm        = toolWindow.getContentManager
    val cf        = cm.getFactory

    cm.addContent(cf.createContent(presenter.getView, "Challenge Description", false))
    cm.addContent(cf.createContent(LogConsoleView(project), LogConsoleView.DISPLAY_NAME, false))
    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")

    val testItem = ChallengeStorageItem()
    testItem.descriptionFilePath = """C:\Sources\Work\CodeEpiphany\testResources\leetcode_demo.html"""
    testItem.dojo = LeetCodeCN
    presenter.updateCurrentQuestion(testItem)
  }
}

object SidebarWindowFactory {
  @static
  final val TOOL_WINDOW_ID = "CodeEpiphany.Sidebar"
}
