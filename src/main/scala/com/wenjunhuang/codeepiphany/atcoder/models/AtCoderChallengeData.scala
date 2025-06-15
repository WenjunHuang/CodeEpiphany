package com.wenjunhuang.codeepiphany.atcoder.models

import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

case class AtCoderChallengeData(
  contestId: String,
  problemId: String,
  description: String,
  supportedLanguages: Set[(Language, LanguageVersion)],
  testCases:List[ChallengeSettings.TestCase]
)
