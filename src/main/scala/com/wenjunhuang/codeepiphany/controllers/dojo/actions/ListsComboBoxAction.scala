package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.LISTS_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.utils.implicits.*

import javax.swing.JComponent

class ListsComboBoxAction extends ComboBoxAction {
  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    LISTS_PROVIDER_KEY.getData(dataContext) match
      case null => DefaultActionGroup()
      case provider =>
        val group = new DefaultActionGroup()
        myAllItems.foreach { item =>
          group.add(new QuestionSheetAction(item))
        }
        group

  override def update(e: AnActionEvent): Unit =
    Option(LISTS_PROVIDER_KEY.getData(e.getDataContext)).foreach { provider =>
      (provider.getAllItems(), provider.getSelectedItems()).parMapN { (allItems, selectedItems) =>
        myAllItems = allItems
        mySelectedItems = selectedItems
        e.getPresentation.setEnabledAndVisible(true)
      }.unsafeRunAndForget()
    }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  private var myAllItems: List[ListQueryItem]      = List()
  private var mySelectedItems: List[ListQueryItem] = List()
}

object ListsComboBoxAction {}

class QuestionSheetAction(private val myItem: ListQueryItem) extends AnAction(myItem.name) {

  override def actionPerformed(e: AnActionEvent): Unit = {}

  override def update(e: AnActionEvent): Unit = {
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}
