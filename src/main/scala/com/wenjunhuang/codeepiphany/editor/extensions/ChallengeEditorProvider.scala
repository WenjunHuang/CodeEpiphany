package com.wenjunhuang.codeepiphany.editor.extensions

import org.jdom.Element

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider.Builder
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.{ DumbAware, Project }
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.editor.actions.{ SolutionSelectionAction, SurroundSubmissionRegionAction }
import com.wenjunhuang.codeepiphany.editor.actions.SolutionSelectionAction.SOLUTION_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.editor.actions.SubmitCodeAction.{ SUBMITCODE_PROVIDER_KEY, SubmitCodeProvider }
import com.wenjunhuang.codeepiphany.editor.actions.SurroundSubmissionRegionAction.SURROUND_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

class ChallengeEditorProvider extends AsyncFileEditorProvider with DumbAware {
  private val delegate = PsiAwareTextEditorProvider()

  override def accept(project: Project, file: VirtualFile): Boolean = {
    if !delegate.accept(project, file) then false
    else
      ChallengeSettings.getInstance(project).findChallengeId(file) match
        case None =>
          false
        case Some(challenge) =>
          true
  }

  override def getPolicy: FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  override def createEditor(project: Project, file: VirtualFile): FileEditor =
    setupEditor(delegate.createEditor(project, file).asInstanceOf[TextEditor], project, file)

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
    override def build(): FileEditor =
      setupEditor(delegate.createEditor(project, file).asInstanceOf[TextEditor], project, file)
  }

  private def setupEditor(editor: TextEditor, project: Project, file: VirtualFile): TextEditor = {
    ChallengeSettings.getInstance(project).findChallengeId(file) match
      case Some(challenge) =>
        val editorWrapper = ChallengeEditor(editor)
        editorWrapper.putUserData(
          SUBMITCODE_PROVIDER_KEY,
          SubmitCodeProvider.createProvider(file, project, challenge.dojo)
        )
        editorWrapper.putUserData(
          SOLUTION_PROVIDER_KEY,
          SolutionSelectionAction.createSolutionSelectionProvider(project, file)
        )
        editorWrapper.putUserData(
          SURROUND_PROVIDER_KEY,
          SurroundSubmissionRegionAction.createProvider(editorWrapper.getEditor, project)
        )
        editorWrapper
      case None =>
        // should never happen
        editor
  }
}
