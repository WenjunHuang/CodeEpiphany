package com.wenjunhuang.codeepiphany.vfs

import com.intellij.openapi.fileTypes.LanguageFileType

import javax.swing.Icon

class SolutionRemarkFileType(private val myIdeaLanguage: com.intellij.lang.Language)
    extends LanguageFileType(myIdeaLanguage) {
  override def getName: String = "SolutionRemark"

  override def getDescription: String = "Solution remark"

  override def getDefaultExtension: String = myIdeaLanguage.getAssociatedFileType.getDefaultExtension

  override def getIcon: Icon = myIdeaLanguage.getAssociatedFileType.getIcon

  override def isReadOnly: Boolean = super.isReadOnly
}
