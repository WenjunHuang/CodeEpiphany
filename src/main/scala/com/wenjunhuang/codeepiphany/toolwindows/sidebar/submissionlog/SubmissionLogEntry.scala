package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import java.time.LocalDateTime

import com.wenjunhuang.codeepiphany.model.*

case class SubmissionLogEntry(
  id: Long,
  dojo: CodeDojo,
  challengeTitle: String,
  solution: String,
  language: Language,
  languageVersion: LanguageVersion,
  difficulty: ChallengeDifficulty,
  resultMessage: String,
  result: SubmissionResult,
  submissionDateTime: Option[LocalDateTime],
  resultDateTime: Option[LocalDateTime]
)
