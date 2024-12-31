package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.SKILL_PROVIDER_KEY

import javax.swing.JComponent

class SkillAction extends ComboBoxAction {
  override def update(e: AnActionEvent): Unit =
    Option(SKILL_PROVIDER_KEY.getData(e.getDataContext)) match
      case None           => e.getPresentation.setEnabled(false)
      case Some(provider) => e.getPresentation.setEnabled(provider.getAllItems.nonEmpty)

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    Option(SKILL_PROVIDER_KEY.getData(dataContext))
      .map(_.getAllItems)
      .map(diffs => diffs.map(SkillSubAction(_)))
      .map(actions => new DefaultActionGroup(actions*))
      .getOrElse(new DefaultActionGroup())

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

class SkillSubAction(private val mySkill: Skill) extends AnAction(mySkill.name) {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(SKILL_PROVIDER_KEY.getData(e.getDataContext)).foreach(_.toggleSelection(mySkill))

  override def update(e: AnActionEvent): Unit =
    Option(SKILL_PROVIDER_KEY.getData(e.getDataContext))
      .map(_.isSelected(mySkill))
      .foreach {
        case true =>
          e.getPresentation.setIcon(AllIcons.Actions.Checked)
        case false => e.getPresentation.setIcon(null)
      }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
