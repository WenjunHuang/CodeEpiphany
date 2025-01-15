package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import com.wenjunhuang.codeepiphany.model.*

import java.time.LocalDateTime

case class SubmissionLogEntry(
  id:Int,
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
