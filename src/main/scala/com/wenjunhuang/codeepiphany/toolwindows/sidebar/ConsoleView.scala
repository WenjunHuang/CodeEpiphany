package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.openapi.actionSystem.{ActionManager, DefaultActionGroup}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.wenjunhuang.codeepiphany.model.Constants

class ConsoleView(private val myProject: Project) extends SimpleToolWindowPanel(false, true) {
  private val myConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole
  
  init()

  private def init(): Unit = {
    val actionGroup = DefaultActionGroup(myConsoleView.createConsoleActions()*)
    val toolbar = ActionManager
      .getInstance()
      .createActionToolbar(Constants.ACTION_PREFIX + ".ConsoleView", actionGroup, true)

    toolbar.setTargetComponent(myConsoleView.getComponent)
    setContent(myConsoleView.getComponent)
    setToolbar(toolbar.getComponent)
  }




}
