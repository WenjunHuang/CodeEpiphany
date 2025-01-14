package com.wenjunhuang.codeepiphany.editor.extensions

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider.Builder
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.editor.actions.SubmitCodeAction.{SUBMITCODE_PROVIDER_KEY, SubmitCodeProvider}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import org.jdom.Element

class ChallengeEditorProvider extends AsyncFileEditorProvider with DumbAware {
  private val delegate = PsiAwareTextEditorProvider()

  override def accept(project: Project, file: VirtualFile): Boolean = {
    if !delegate.accept(project, file) then false
    else
      val path = file.getCanonicalPath
      ChallengeSettings.getInstance(project).findChallengeId(path) match
        case None =>
          false
        case Some(challenge) =>
          true
  }

  override def getPolicy: FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  override def createEditor(project: Project, file: VirtualFile): FileEditor =
    setupEditor(delegate.createEditor(project, file), project, file)

  override def readState(element: Element, project: Project, file: VirtualFile): FileEditorState = {
    delegate.readState(element, project, file)
  }

  override def writeState(state: FileEditorState, project: Project, element: Element): Unit = {
    delegate.writeState(state, project, element)
  }

  override def getEditorTypeId: String = {
    s"CodeEpiphany.${delegate.getEditorTypeId}"
  }

  override def createEditorAsync(project: Project, file: VirtualFile): AsyncFileEditorProvider.Builder = new Builder() {
    override def build(): FileEditor = setupEditor(delegate.createEditor(project, file), project, file)
  }

  private def setupEditor(editor: FileEditor, project: Project, file: VirtualFile): FileEditor = {
    editor.putUserData(SUBMITCODE_PROVIDER_KEY, SubmitCodeProvider.createProvider(file, project))
    editor
  }
}
