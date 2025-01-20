package com.wenjunhuang.codeepiphany.editor.actions

import org.typelevel.ci.CIString

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnActionEvent, CommonDataKeys, DefaultActionGroup }

import com.wenjunhuang.codeepiphany.actions.LoginAction
import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, CodeDojo }
import com.wenjunhuang.codeepiphany.services.AuthService
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.PluginBundle

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
            Option(
              repository.getDSLContext
                .select(CHALLENGE.DOJO)
                .from(CHALLENGE)
                .where(CHALLENGE.ID.eq(challenge.challengeId))
                .fetchOne()
            ).flatMap(r => Option(r.value1()))
              .flatMap(v => CodeDojo.fromCIString(CIString(v)))
              .foreach { codeDojo =>
                presentation.setIcon(codeDojo.getIcon.orNull)
                if AuthService.getInstance(project).isLoggedIn(codeDojo) then presentation.setEnabled(true)
                else presentation.setText(PluginBundle.message("group.CodeEpiphany.Editor.Menu.login"))
              }
          case _ =>
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
