package com.wenjunhuang.codeepiphany.vfs

import com.intellij.openapi.fileEditor.{FileEditor, FileEditorPolicy}
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.utils.walkaround.FileEditorProviderBridge

class SubmissionCodeEditorProvider extends FileEditorProviderBridge with DumbAware {
  private val delegate = PsiAwareTextEditorProvider()

  override def accept(project: Project, file: VirtualFile): Boolean = file.isInstanceOf[SubmissionCodeFile]

  override def getPolicy: FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  override def createEditor(project: Project, file: VirtualFile): FileEditor =
    delegate.createEditor(project, file)

  override def getEditorTypeId: String = {
    s"CodeEpiphany.SubmissionCode.${delegate.getEditorTypeId}"
  }
}
