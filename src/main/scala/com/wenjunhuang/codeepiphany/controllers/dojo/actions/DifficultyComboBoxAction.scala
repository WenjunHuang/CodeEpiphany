package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent, DataContext, DefaultActionGroup}
import com.intellij.openapi.actionSystem.ex.ComboBoxAction

import javax.swing.JComponent

class DifficultyComboBoxAction extends ComboBoxAction {
  
  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabledAndVisible(true)

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup = {
    val group = new DefaultActionGroup()
    group.add(new DifficultyAction("Easy"))
    group.add(new DifficultyAction("Medium"))
    group.add(new DifficultyAction("Hard"))
    group
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

class DifficultyAction(val difficulty: String) extends AnAction(difficulty) {
  override def actionPerformed(e: AnActionEvent): Unit = ()
}
