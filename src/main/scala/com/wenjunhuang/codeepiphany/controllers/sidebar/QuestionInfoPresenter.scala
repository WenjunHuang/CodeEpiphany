package com.wenjunhuang.codeepiphany.controllers.sidebar

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.content.ContentManager
import com.wenjunhuang.codeepiphany.model.QuestionStorage

class QuestionInfoPresenter(val project: Project) extends Disposable {
  private val editorListener = QuestionFileEditorListener()

  def createUIForFile(contentManager: ContentManager,file:VirtualFile): Unit = {
    val storage = QuestionStorage.getInstance(project)
    storage.findQuestionItemByFilePath(file.getPath) match
      case None =>
        contentManager.removeAllContents(false)
      case Some(questionItem) =>
  }

  override def dispose(): Unit                       = {}
}
