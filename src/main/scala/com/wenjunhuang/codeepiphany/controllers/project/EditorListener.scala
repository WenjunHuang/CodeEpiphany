package com.wenjunhuang.codeepiphany.controllers.project

import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerEvent, FileEditorManagerListener}
import com.intellij.openapi.vfs.VirtualFile

class EditorListener extends FileEditorManagerListener {
  override def fileOpened(source: FileEditorManager, file: VirtualFile): Unit = super.fileOpened(source, file)

  override def fileClosed(source: FileEditorManager, file: VirtualFile): Unit = super.fileClosed(source, file)

  override def selectionChanged(event: FileEditorManagerEvent): Unit = super.selectionChanged(event)
}
