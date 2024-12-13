package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.LISTS_PROVIDER_KEY

import javax.swing.JComponent

class ListsAction extends ComboBoxAction {
  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    LISTS_PROVIDER_KEY.getData(dataContext) match
      case null => DefaultActionGroup()
      case provider =>
        DefaultActionGroup(provider.getAllItems.map(item => new ListsItemAction(item))*)

  override def update(e: AnActionEvent): Unit =
    Option(LISTS_PROVIDER_KEY.getData(e.getDataContext)) match
      case None           => e.getPresentation.setEnabled(false)
      case Some(provider) => e.getPresentation.setEnabled(provider.getAllItems.nonEmpty)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

class ListsItemAction(private val myItem: ListQueryItem) extends AnAction(myItem.name) {

  override def actionPerformed(e: AnActionEvent): Unit =
    Option(LISTS_PROVIDER_KEY.getData(e.getDataContext)).foreach(_.toggleSelection(myItem))

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    Option(LISTS_PROVIDER_KEY.getData(e.getDataContext))
      .map(_.isSelected(myItem))
      .foreach {
        case true  => presentation.setIcon(AllIcons.Actions.Checked)
        case false => presentation.setIcon(null)
      }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
