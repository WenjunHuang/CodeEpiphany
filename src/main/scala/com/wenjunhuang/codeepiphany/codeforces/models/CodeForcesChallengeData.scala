package com.wenjunhuang.codeepiphany.codeforces.models

import com.wenjunhuang.codeepiphany.model.{ Language, LanguageVersion }

case class CodeForcesChallengeData(
  contestId: Long,
  index: String,
  description: String,
  supportedLanguages: Set[(Language, LanguageVersion)]
)
