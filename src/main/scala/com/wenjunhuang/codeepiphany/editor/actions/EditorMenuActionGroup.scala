package com.wenjunhuang.codeepiphany.editor.actions

import com.intellij.openapi.actionSystem.{
  ActionUpdateThread,
  AnActionEvent,
  CommonDataKeys,
  DefaultActionGroup,
  PlatformDataKeys
}
import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, CodeDojo }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import org.typelevel.ci.CIString

class EditorMenuActionGroup extends DefaultActionGroup {
  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    if project == null then e.getPresentation.setEnabledAndVisible(false)
    else
      val vf = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext)
      if vf != null then
        ChallengeSettings.getInstance(project).findChallengeId(vf.getCanonicalPath) match {
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
            e.getPresentation.setEnabledAndVisible(true)
            e.getPresentation.setIcon(icon)
          case None => e.getPresentation.setEnabledAndVisible(false)
        }
      else e.getPresentation.setEnabledAndVisible(false)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
