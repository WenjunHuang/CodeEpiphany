package com.wenjunhuang.codeepiphany.vfs

import com.intellij.openapi.fileEditor.{ FileEditor, FileEditorPolicy }
import com.intellij.openapi.project.{ DumbAware, Project }
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.utils.walkaround.FileEditorProviderBridge

class WebPreviewEditorProvider extends FileEditorProviderBridge with DumbAware {

  override def accept(project: Project, file: VirtualFile): Boolean = file.isInstanceOf[WebPreviewVirtualFile]

  override def acceptRequiresReadAction(): Boolean = false

  override def createEditor(project: Project, file: VirtualFile): FileEditor = {
    val editor = WebPreviewFileEditor(file.asInstanceOf[WebPreviewVirtualFile])
    editor.reloadPage()
    editor
  }

  override def getEditorTypeId: String = s"CodeEpiphany.WebPreviewEditor"

  override def getPolicy: FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
