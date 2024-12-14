package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent, DataContext, DefaultActionGroup }
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.PAGINATION_PROVIDER_KEY

import javax.swing.JComponent

class PageSizeAction extends ComboBoxAction {
  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    Option(PAGINATION_PROVIDER_KEY.getData(dataContext)) match {
      case None => DefaultActionGroup()
      case Some(provider) =>
        DefaultActionGroup(provider.getAllItems.map(item => new RangePageSizeItemAction(item))*)
    }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(e: AnActionEvent): Unit =
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case Some(provider) =>
        val presentation = e.getPresentation
        presentation.setEnabled(true)
        provider.getSelectedItems.headOption match {
          case None       => presentation.setText("")
          case Some(item) => presentation.setText(item.name)
        }
    }
}

class RangePageSizeItemAction(private val myItem: QueryPageSizeItem) extends AnAction(myItem.name) {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext)).foreach(_.toggleSelection(myItem))

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    Option(PAGINATION_PROVIDER_KEY.getData(e.getDataContext))
      .map(_.isSelected(myItem))
      .foreach {
        case true  => presentation.setIcon(AllIcons.Actions.Checked)
        case false => presentation.setIcon(null)
      }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
