package com.wenjunhuang.codeepiphany.vfs

import com.intellij.openapi.fileEditor.{AsyncFileEditorProvider, FileEditor, FileEditorPolicy}
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider.Builder
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.vfs.VirtualFile

class SubmissionCodeEditorProvider extends AsyncFileEditorProvider with DumbAware {
  private val delegate = PsiAwareTextEditorProvider()

  override def accept(project: Project, file: VirtualFile): Boolean = file.isInstanceOf[SubmissionCodeFile]

  override def getPolicy: FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  override def createEditor(project: Project, file: VirtualFile): FileEditor =
    delegate.createEditor(project, file)

  override def getEditorTypeId: String = {
    s"CodeEpiphany.SubmissionCode.${delegate.getEditorTypeId}"
  }

  override def createEditorAsync(project: Project, file: VirtualFile): AsyncFileEditorProvider.Builder = new Builder() {
    override def build(): FileEditor = delegate.createEditor(project, file)
  }
}
