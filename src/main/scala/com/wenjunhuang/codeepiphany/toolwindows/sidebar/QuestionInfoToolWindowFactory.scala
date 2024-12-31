package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory, ToolWindowManager}
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.wenjunhuang.codeepiphany.model.ChallengeStorage.Challenge
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCodeCN
import kotlin.coroutines.Continuation

class QuestionInfoToolWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val presenter = DescriptionPresenter(project)
    val cm        = toolWindow.getContentManager
    val cf        = cm.getFactory

    cm.addContent(cf.createContent(presenter.getView, "Question Description", false))
    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")

    val testItem = Challenge()
    testItem.descriptionFilePath = """C:\Sources\Work\CodeEpiphany\leetcode_demo.html"""
    testItem.dojo = LeetCodeCN
    presenter.updateCurrentQuestion(testItem)
  }
}
