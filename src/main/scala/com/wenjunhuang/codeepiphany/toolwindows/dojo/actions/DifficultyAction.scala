package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.DIFFICULTIES_PROVIDER_KEY

import javax.swing.JComponent

class DifficultyAction extends ComboBoxAction {

  override def update(e: AnActionEvent): Unit =
    Option(DIFFICULTIES_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => e.getPresentation.setEnabled(false)
      case _ => e.getPresentation.setEnabled(true)

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    Option(DIFFICULTIES_PROVIDER_KEY.getData(dataContext))
      .map(_.getAllItems)
      .map(diffs => diffs.map(new DifficultySubAction(_)))
      .map(actions => new DefaultActionGroup(actions*))
      .getOrElse(new DefaultActionGroup())

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

class DifficultySubAction(val myDifficulty: DifficultyData) extends AnAction(myDifficulty.name) {

  override def actionPerformed(e: AnActionEvent): Unit =
    Option(DIFFICULTIES_PROVIDER_KEY.getData(e.getDataContext))
      .foreach(_.toggleSelection(myDifficulty))

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    presentation.setText(myDifficulty.name)
    Option(DIFFICULTIES_PROVIDER_KEY.getData(e.getDataContext))
      .map(_.isSelected(myDifficulty))
      .foreach {
        case true =>
          presentation.setIcon(AllIcons.Actions.Checked)
        case false => presentation.setIcon(null)
      }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
