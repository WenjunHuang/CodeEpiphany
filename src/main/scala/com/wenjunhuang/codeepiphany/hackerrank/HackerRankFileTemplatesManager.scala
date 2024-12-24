package com.wenjunhuang.codeepiphany.hackerrank

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.openapi.components.Service
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}

@Service
class HackerRankFileTemplatesManager {
  def getTemplate(lang: Language, ver: LanguageVersion): Option[FileTemplate] = None
}

object HackerRankFileTemplatesManager {
  def getInstance(): HackerRankFileTemplatesManager =
    HackerRankFileTemplatesManager()
}
