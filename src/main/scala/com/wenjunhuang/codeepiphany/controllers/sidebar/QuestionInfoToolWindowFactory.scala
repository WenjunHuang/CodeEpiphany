package com.wenjunhuang.codeepiphany.controllers.sidebar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCodeCN
import com.wenjunhuang.codeepiphany.model.QuestionStorage.QuestionItem

class QuestionInfoToolWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val presenter = DescriptionPresenter(project)
    val cm        = toolWindow.getContentManager
    val cf        = cm.getFactory

    cm.addContent(cf.createContent(presenter.getView.getComponent, "Question Description", false))

    val testItem = QuestionItem()
    testItem.descriptionFilePath = """C:\Sources\Work\CodeEpiphany\src\main\scala\com\wenjunhuang\codeepiphany\controllers\sidebar\resources\leetcode_demo.html"""
    testItem.dojo = LeetCodeCN
    presenter.updateCurrentQuestion(testItem)
  }
}
