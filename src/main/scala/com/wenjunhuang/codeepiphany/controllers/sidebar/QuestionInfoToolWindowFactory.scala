package com.wenjunhuang.codeepiphany.controllers.sidebar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory, ToolWindowManager}
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCodeCN
import com.wenjunhuang.codeepiphany.model.QuestionStorage.QuestionItem
import kotlin.coroutines.Continuation

class QuestionInfoToolWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val presenter = DescriptionPresenter(project)
    val cm        = toolWindow.getContentManager
    val cf        = cm.getFactory

    cm.addContent(cf.createContent(presenter.getView, "Question Description", false))

    val testItem = QuestionItem()
    testItem.descriptionFilePath = """C:\Sources\Work\CodeEpiphany\leetcode_demo.html"""
    testItem.dojo = LeetCodeCN
    presenter.updateCurrentQuestion(testItem)
  }
}
