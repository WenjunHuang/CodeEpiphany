package com.wenjunhuang.codeepiphany.actions

import javax.swing.JComponent

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent, Presentation}
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.ui.SearchTextField

class SearchTextAction extends AnAction with CustomComponentAction {
  override def actionPerformed(e: AnActionEvent): Unit = {}

  override def createCustomComponent(presentation: Presentation, place: String): JComponent = {
    SearchTextField(true)
  }

  override def updateCustomComponent(component: JComponent, presentation: Presentation): Unit =
    super.updateCustomComponent(component, presentation)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
