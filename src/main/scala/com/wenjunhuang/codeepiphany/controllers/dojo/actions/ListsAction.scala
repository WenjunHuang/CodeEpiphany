package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.LISTS_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.utils.implicits.*

import javax.swing.JComponent

class ListsAction extends ComboBoxAction {
  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    LISTS_PROVIDER_KEY.getData(dataContext) match
      case null => DefaultActionGroup()
      case provider =>
        val group = new DefaultActionGroup()
        provider.getAllItems.foreach { item =>
          group.add(new ListsItemAction(item))
        }
        group

  override def update(e: AnActionEvent): Unit =
    Option(LISTS_PROVIDER_KEY.getData(e.getDataContext)) match
      case None           => e.getPresentation.setEnabled(false)
      case Some(provider) => e.getPresentation.setEnabled(provider.getAllItems.nonEmpty)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object ListsAction {}

class ListsItemAction(private val myItem: ListQueryItem) extends AnAction(myItem.name) {

  override def actionPerformed(e: AnActionEvent): Unit = {}

  override def update(e: AnActionEvent): Unit = {}

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
