package com.wenjunhuang.codeepiphany.vfs

import java.net.HttpCookie

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

class WebPreviewVirtualFile(
  private val myPreviewUrl: String,
  private val myDomain: String,
  private val myCookies: List[HttpCookie],
  private val myTitle: String
) extends LightVirtualFile {
  setFileType(WebPreviewFileType)
  setWritable(false)

  override def getName: String = myTitle

  def getPreviewUrl: String        = myPreviewUrl
  def getDomain: String            = myDomain
  def getCookies: List[HttpCookie] = myCookies
  def getTitle: String             = myTitle

  override def hashCode(): Int = myPreviewUrl.hashCode

  override def equals(obj: Any): Boolean = {
    obj.asInstanceOf[Matchable] match {
      case that: WebPreviewVirtualFile =>
        (that eq this) || (this.myPreviewUrl == that.myPreviewUrl &&
          this.myDomain == that.myDomain &&
          this.myTitle == that.myTitle)
      case _ => false
    }
  }
}

object WebPreviewVirtualFile {
  def openEditor(file: WebPreviewVirtualFile, project: Project): Unit = {
    val editorManager = FileEditorManager.getInstance(project)
    if (editorManager.isFileOpen(file)) {
      editorManager.getEditors(file).toList match {
        case (editor: WebPreviewFileEditor) :: Nil =>
          editor.reloadPage()
          editorManager.openFile(file, true, true)
        case _ =>
      }
    } else {
      editorManager.openFile(file, true)
    }
  }
}
