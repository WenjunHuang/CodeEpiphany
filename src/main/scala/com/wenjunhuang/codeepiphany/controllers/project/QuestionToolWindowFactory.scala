package com.wenjunhuang.codeepiphany.controllers.project

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.wenjunhuang.codeepiphany.ui.assets.Icons

class QuestionToolWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val contentManager = toolWindow.getContentManager
    val contentFactory = contentManager.getFactory
    val content1       = contentFactory.createContent(JBLabel("Hello World!"), "Hello", false)
    content1.setIcon(Icons.QuestionToolWindowIcon)

    val content2 = contentFactory.createContent(JBLabel("Hello World!"), "Hello", false)
    content2.setIcon(AllIcons.General.LinkDropTriangle)

    contentManager.addContent(content1)
    contentManager.addContent(content2)
  }
}
