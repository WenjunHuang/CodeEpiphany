package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.actionSystem.*
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.STATUS_PROVIDER_KEY

import javax.swing.JComponent

class StatusAction extends ComboBoxAction {
  override def update(e: AnActionEvent): Unit =
    Option(STATUS_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case _    => e.getPresentation.setEnabled(true)
    }

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    Option(STATUS_PROVIDER_KEY.getData(dataContext)) match {
      case None => DefaultActionGroup()
      case Some(provider) =>
        DefaultActionGroup(provider.getAllItems.map(item => new StatusSubAction(item))*)
    }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

class StatusSubAction(private val myStatus: Status) extends AnAction(myStatus.name) {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(STATUS_PROVIDER_KEY.getData(e.getDataContext)).foreach(_.toggleSelection(myStatus))

  override def update(e: AnActionEvent): Unit =
    Option(STATUS_PROVIDER_KEY.getData(e.getDataContext))
      .map(_.isSelected(myStatus))
      .foreach {
        case true =>
          e.getPresentation.setIcon(AllIcons.Actions.Checked)
        case false => e.getPresentation.setIcon(null)
      }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
