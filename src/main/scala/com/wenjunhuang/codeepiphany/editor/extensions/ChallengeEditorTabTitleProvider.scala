package com.wenjunhuang.codeepiphany.editor.extensions

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.services.ChallengeRepository
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

class ChallengeEditorTabTitleProvider extends EditorTabTitleProvider {
  override def getEditorTabTitle(project: Project, file: VirtualFile): String = {
    if project == null then null
    else
      val settings = ChallengeSettings.getInstance(project)
      settings.findChallengeId(file.getCanonicalPath) match {
        case Some(item) =>
          Option(
            ChallengeRepository
              .getInstance(project)
              .getDSLContext
              .select(CHALLENGE.TITLE)
              .from(CHALLENGE)
              .where(CHALLENGE.ID.eq(item.challengeId))
              .fetchOne()
          ).flatMap(r => Option(r.value1())) match
            case None        => null
            case Some(title) => title
        case None => null
      }
  }
}
