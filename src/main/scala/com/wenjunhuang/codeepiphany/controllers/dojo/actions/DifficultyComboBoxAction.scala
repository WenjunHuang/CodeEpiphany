package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent, DataContext, DefaultActionGroup, Presentation}
import com.intellij.openapi.actionSystem.ex.{CheckboxAction, ComboBoxAction}
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.DIFFICULTIES_PROVIDER_KEY
import icons.CodeEpiphanyIcons

import javax.swing.JComponent

class DifficultyComboBoxAction extends ComboBoxAction {

  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabledAndVisible(true)

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup = {
    Option(DIFFICULTIES_PROVIDER_KEY.getData(dataContext))
      .map(_.getDifficulties)
      .map(diffs => diffs.map(new DifficultyAction(_)))
      .map(actions => new DefaultActionGroup(actions*))
      .getOrElse(new DefaultActionGroup())

  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

class DifficultyAction(val myDifficulty: Difficulty) extends AnAction(myDifficulty.name) {

  override def actionPerformed(e: AnActionEvent): Unit = {
    println("hello")
  }

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    e.getPresentation.setEnabled(true)
    e.getPresentation.setIcon(AllIcons.General.GreenCheckmark)
//    presentation.putClientProperty("html.disable", false)
    presentation.setText(s"<html><font color='red'>${presentation.getText}</font></html>")
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
