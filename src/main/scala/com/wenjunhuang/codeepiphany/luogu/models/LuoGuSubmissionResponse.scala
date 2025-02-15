package com.wenjunhuang.codeepiphany.luogu.models

import com.wenjunhuang.codeepiphany.model.SubmissionResult

case class LuoGuSubmissionResponse(
  submissionId: String,
  result: SubmissionResult,
  message:String
)
