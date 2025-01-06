package com.wenjunhuang.codeepiphany.editor.extensions

import com.intellij.ide.DataManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.editor.actions.providers.SubmitCodeProvider
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

class ChallengeEditorProvider extends PsiAwareTextEditorProvider {
  override def accept(project: Project, file: VirtualFile): Boolean =  {
    if !super.accept(project, file) then false
    else
      val path = file.getCanonicalPath
      ChallengeSettings.getInstance(project).findChallengeId(path) match
        case None => false
        case Some(challenge) => true
  }

  override def createEditor(project: Project, file: VirtualFile): FileEditor = {
    val editor = super.createEditor(project, file)

    editor
  }
}
