package com.wenjunhuang.codeepiphany.editor

import com.intellij.ide.FileIconPatcher
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.{CodeDojo, ChallengeRepository}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import org.typelevel.ci.CIString

import javax.swing.Icon

class ChallengeEditorIconProvider extends FileIconPatcher {
  override def patchIcon(icon: Icon, file: VirtualFile, flags: Int, project: Project): Icon = {
    if project == null then icon
    else
      val settings = ChallengeSettings.getInstance(project)
      settings.findChallengeId(file.getCanonicalPath) match {
        case Some(item) =>
          val repository = ChallengeRepository.getInstance(project)
          Option(
            repository.getDSLContext
              .select(CHALLENGE.DOJO)
              .from(CHALLENGE)
              .where(CHALLENGE.ID.eq(item.challengeId))
              .fetchOne()
          ).flatMap(r => Option(r.value1()))
            .flatMap(v => CodeDojo.fromCIString(CIString(v)))
            .flatMap(_.getIcon) match
            case None       => icon
            case Some(dojo) => dojo
        case None => icon
      }
  }
}
