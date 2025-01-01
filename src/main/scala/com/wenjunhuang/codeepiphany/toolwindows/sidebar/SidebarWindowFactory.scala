package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ ToolWindow, ToolWindowFactory, ToolWindowManager }
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.wenjunhuang.codeepiphany.model.ChallengeStorage.Challenge
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCodeCN
import kotlin.coroutines.Continuation

class SidebarWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val presenter = ChallengeDescriptionPresenter(project)
    val cm        = toolWindow.getContentManager
    val cf        = cm.getFactory

    cm.addContent(cf.createContent(presenter.getView, "Challenge Description", false))
    cm.addContent(cf.createContent(ConsoleView(project), "Console", false))
    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")

    val testItem = Challenge()
    testItem.descriptionFilePath = """C:\Sources\Work\CodeEpiphany\testResources\leetcode_demo.html"""
    testItem.dojo = LeetCodeCN
    presenter.updateCurrentQuestion(testItem)
  }
}
