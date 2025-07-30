package com.wenjunhuang.codeepiphany.vfs

import java.beans.PropertyChangeListener
import javax.swing.JComponent

import com.intellij.openapi.fileEditor.{ FileEditor, FileEditorState }
import com.intellij.openapi.util.{ Disposer, UserDataHolderBase }
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.ui.jcef.JBCefCookie

class WebPreviewFileEditor(private val myFile: WebPreviewVirtualFile) extends UserDataHolderBase with FileEditor {
  private val myPanel: JCEFHtmlPanel = new JCEFHtmlPanel(myFile.getPreviewUrl) {
    myFile.getCookies.foreach { cookie =>
      val jbcefCookie =
        JBCefCookie(cookie.getName, cookie.getValue, myFile.getDomain, "/", true, false)
      getJBCefCookieManager
        .setCookie(myFile.getPreviewUrl, jbcefCookie)
        .cancel(true)
    }
  }

  def reloadPage():Unit = {
    myPanel.loadURL(myFile.getPreviewUrl)
  }

  override def setState(state: FileEditorState): Unit = {}

  override def getFile: VirtualFile = myFile

  override def getComponent: JComponent = myPanel.getComponent

  override def getPreferredFocusedComponent: JComponent = myPanel.getComponent

  override def isModified: Boolean = false

  override def isValid: Boolean = true

  override def addPropertyChangeListener(listener: PropertyChangeListener): Unit = {}

  override def removePropertyChangeListener(listener: PropertyChangeListener): Unit = {}

  override def getName: String = myFile.getTitle

  override def dispose(): Unit = {
    Disposer.dispose(myPanel)
  }
}
