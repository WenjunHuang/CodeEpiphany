package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerEvent, FileEditorManagerListener}
import com.intellij.openapi.vfs.VirtualFile

/** Monitor the current file selection changes and update the question sidebar accordingly.
  */
class ChallengeCodeFileEditorListener extends FileEditorManagerListener {
  override def selectionChanged(event: FileEditorManagerEvent): Unit = {
  }

  override def fileOpened(source: FileEditorManager, file: VirtualFile): Unit = super.fileOpened(source, file)

  override def fileClosed(source: FileEditorManager, file: VirtualFile): Unit = super.fileClosed(source, file)
}
