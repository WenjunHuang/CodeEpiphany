package com.wenjunhuang.codeepiphany.vfs

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.ex.FakeFileType
import com.intellij.openapi.vfs.VirtualFile

object WebPreviewFileType extends FakeFileType{
  override def getName: String = "WebPreviewFileType"

  override def getDescription: String = "Web Preview File Type"

  override def getDisplayName: String = "Web Preview"

  override def isMyFileType(file:VirtualFile):Boolean = file.isInstanceOf[WebPreviewVirtualFile]

  override def getIcon: javax.swing.Icon = AllIcons.Nodes.PpWeb
}
