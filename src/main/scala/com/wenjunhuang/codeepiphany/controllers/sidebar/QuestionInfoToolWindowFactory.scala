package com.wenjunhuang.codeepiphany.controllers.sidebar

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.wenjunhuang.codeepiphany.utils.Icons

class QuestionInfoToolWindowFactory extends ToolWindowFactory() {

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    toolWindow.getContentManager.getFactory
    
  }
}
