package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import com.wenjunhuang.codeepiphany.model.{CodeDojo, Difficulty, Language, LanguageVersion, SubmissionResult}

import java.time.LocalDateTime

case class SubmissionLogEntry(
  dojo: CodeDojo,
  challengeTitle: String,
  solution: String,
  language:Language,
  languageVersion: LanguageVersion,
  difficulty: Difficulty,
  resultMessage: String,
  result: SubmissionResult,
  submissionDateTime: LocalDateTime,
  resultDateTime: LocalDateTime
)
