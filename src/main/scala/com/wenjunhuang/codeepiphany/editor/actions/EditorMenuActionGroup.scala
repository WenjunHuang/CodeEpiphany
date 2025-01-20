package com.wenjunhuang.codeepiphany.editor.actions

import org.typelevel.ci.CIString

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnActionEvent, CommonDataKeys, DefaultActionGroup }

import com.wenjunhuang.codeepiphany.actions.LoginAction
import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, CodeDojo }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

class EditorMenuActionGroup extends DefaultActionGroup {
  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    presentation.setEnabled(false)

    val project = e.getProject
    if project != null then
      val vf = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext)
      if vf != null then
        ChallengeSettings.getInstance(project).findChallengeId(vf.getCanonicalPath) match
          case Some(challenge) =>
            val repository = ChallengeRepository.getInstance(project)
            val icon = Option(
              repository.getDSLContext
                .select(CHALLENGE.DOJO)
                .from(CHALLENGE)
                .where(CHALLENGE.ID.eq(challenge.challengeId))
                .fetchOne()
            ).flatMap(r => Option(r.value1()))
              .flatMap(v => CodeDojo.fromCIString(CIString(v)))
              .flatMap(_.getIcon) match
              case None       => null
              case Some(icon) => icon
            presentation.setEnabled(true)
            presentation.setIcon(icon)
          case _ =>
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
