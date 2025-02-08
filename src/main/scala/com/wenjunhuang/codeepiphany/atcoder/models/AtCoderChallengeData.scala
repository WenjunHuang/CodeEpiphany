package com.wenjunhuang.codeepiphany.atcoder.models

import com.wenjunhuang.codeepiphany.model.{ Language, LanguageVersion }

case class AtCoderChallengeData(
  contestId: String,
  problemId: String,
  description: String,
  supportedLanguages: Set[(Language, LanguageVersion)]
)
