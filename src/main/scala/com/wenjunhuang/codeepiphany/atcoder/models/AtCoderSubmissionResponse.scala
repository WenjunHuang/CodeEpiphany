package com.wenjunhuang.codeepiphany.atcoder.models

import java.time.LocalDateTime

import com.wenjunhuang.codeepiphany.model.SubmissionResult

case class AtCoderSubmissionResponse(
  submissionId: String,
  contestId: String,
  score: Int,
  result: SubmissionResult,
  message:String
)
