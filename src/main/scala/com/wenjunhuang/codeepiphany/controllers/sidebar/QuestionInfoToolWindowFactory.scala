package com.wenjunhuang.codeepiphany.controllers.sidebar

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.{ ToolWindow, ToolWindowFactory, ToolWindowManager }
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.wenjunhuang.codeepiphany.utils.Icons

class QuestionInfoToolWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val presenter = DescriptionPresenter(project)
    val cm        = toolWindow.getContentManager
    val cf        = cm.getFactory

    cm.addContent(cf.createContent(presenter.getView.getComponent, "Question Description", false))
    presenter.loadUrl("https://www.bilibili.com/")
  }
}
