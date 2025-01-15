package com.wenjunhuang.codeepiphany.vfs

import com.intellij.openapi.fileTypes.{FileTypeManager, LanguageFileType}
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}

import javax.swing.Icon

class SubmissionCodeFileType(
  private val myLanguage: Language,
  private val myLanguageVersion: LanguageVersion,
  private val myIdeaLanguage: com.intellij.lang.Language
) extends LanguageFileType(myIdeaLanguage) {
  override def getName: String = "SubmissionCode"

  override def getDescription: String = "Submission Code"

  override def getDefaultExtension: String = myLanguage.fileExt

  override def getIcon: Icon = FileTypeManager.getInstance.getFileTypeByExtension(myLanguage.fileExt) match
    case languageFileType: LanguageFileType => languageFileType.getIcon
    case _                                  => myLanguage.icon

  override def isReadOnly: Boolean = true
}
