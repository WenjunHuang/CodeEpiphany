package com.wenjunhuang.codeepiphany.editor

import com.intellij.ide.FileIconPatcher
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.LayeredIcon
import com.intellij.util.IconUtil
import icons.CodeEpiphanyIcons

import javax.swing.{ Icon, SwingConstants }

class ChallengeEditorIconProvider extends FileIconPatcher {
  override def patchIcon(icon: Icon, file: VirtualFile, flags: Int, project: Project): Icon = {
    if project == null then icon
    else if file.getName.startsWith("Birth") then CodeEpiphanyIcons.Dojos.HACKERRANK
    else icon
  }
}
