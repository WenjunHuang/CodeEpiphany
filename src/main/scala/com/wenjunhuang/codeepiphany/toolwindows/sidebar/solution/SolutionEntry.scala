package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solution

import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}

enum SolutionEntry {
  case LanguageNode(language: Language, languageVersion: LanguageVersion, submissionCount: Int)
  case SolutionNode(solutionId: Long, title: String, submissionCount: Int,isDefault:Boolean)
}
