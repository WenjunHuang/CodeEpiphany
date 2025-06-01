package com.wenjunhuang.codeepiphany.toolwindows.sidebar.notes

import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}

enum NoteEntry {
  case LanguageNode(language: Language, languageVersion: LanguageVersion, submissionCount: Int)
  case NoteNode(solutionId: Long, title: String, submissionCount: Int, isDefault:Boolean)
}
