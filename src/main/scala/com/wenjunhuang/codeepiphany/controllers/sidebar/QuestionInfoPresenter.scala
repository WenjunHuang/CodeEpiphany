package com.wenjunhuang.codeepiphany.controllers.sidebar

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentManager
import com.wenjunhuang.codeepiphany.model.QuestionStorage
import org.jetbrains.annotations.NotNull
import cats.syntax.all.*

class QuestionInfoPresenter(val project: Project) extends Disposable {
  private var toolWindow: Option[ToolWindow] = None

  def updateSelection(file: VirtualFile): Unit = {
    (toolWindow,QuestionStorage.getInstance(project).findQuestionItemByFilePath(file.getPath)).mapN{
      case (tw,qi) =>
        ???
    }
  }

  def attachToolWindow(@NotNull tw: ToolWindow): Unit =
    if toolWindow.isEmpty then toolWindow = Option(tw)

  override def dispose(): Unit = {}
}

object QuestionInfoPresenter {
  def getInstance(project: Project): QuestionInfoPresenter = project.getService(classOf[QuestionInfoPresenter])
}
