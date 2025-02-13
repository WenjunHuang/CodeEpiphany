package com.wenjunhuang.codeepiphany.editor.extensions

import javax.swing.Icon

import com.intellij.ide.FileIconPatcher
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.editor.extensions.ChallengeEditorIconProvider.{FILE_ICON_KEY, FileIcon}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

class ChallengeEditorIconProvider extends FileIconPatcher {
  override def patchIcon(icon: Icon, file: VirtualFile, flags: Int, project: Project): Icon = {
    if project == null then icon
    else
      file.getUserData(FILE_ICON_KEY) match
        case null =>
          val settings = ChallengeSettings.getInstance(project)
          settings.findChallengeId(file) match {
            case Some(item) =>
              file.putUserData(FILE_ICON_KEY, FileIcon(item.dojo.getIcon))
              item.dojo.getIcon.orNull
            case None =>
              file.putUserData(FILE_ICON_KEY, FileIcon(None))
              icon
          }
        case FileIcon(patched) => patched.getOrElse(icon)
  }
}

object ChallengeEditorIconProvider {
  case class FileIcon(icon: Option[Icon])
  val FILE_ICON_KEY: Key[FileIcon] = Key.create[FileIcon]("FILE_ICON_KEY")
}
