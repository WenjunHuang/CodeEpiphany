package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionGroup, ActionUpdateThread, AnAction, AnActionEvent, DataContext, DefaultActionGroup}
import com.intellij.openapi.actionSystem.ex.ComboBoxAction

import javax.swing.{Icon, JComponent}

class StatusComboBoxAction extends ComboBoxAction {
  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabledAndVisible(true)

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup = {
    val group = new DefaultActionGroup()
    group.add(new StatusAction("Not Started", "Not Started", AllIcons.General.TodoDefault))
    group.add(new StatusAction("In Progress", "In Progress", AllIcons.General.TodoQuestion))
    group.add(new StatusAction("Completed", "Completed", AllIcons.General.TodoImportant))
    group
  }
}

class StatusAction(val text: String, val description: String, val icon: Icon) extends AnAction(text, description, icon) {
  override def actionPerformed(e: AnActionEvent): Unit = {}
}
