package com.wenjunhuang.codeepiphany.atcoder.models

import java.time.LocalDateTime

import com.wenjunhuang.codeepiphany.model.SubmissionResult

case class AtCoderSubmissionResponse(
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
