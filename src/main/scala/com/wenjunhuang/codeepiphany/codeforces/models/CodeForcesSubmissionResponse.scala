package com.wenjunhuang.codeepiphany.codeforces.models

import com.wenjunhuang.codeepiphany.model.SubmissionResult

import java.time.LocalDateTime

case class CodeForcesSubmissionResponse(
  submissionId: Long,
  when: LocalDateTime,
  who: String,
  problemContestIdIndex: String,
  problemName: String,
  lang: String,
  verdict: String,
  time: String,
  memory: String,
  result: SubmissionResult,
  message: String
)
