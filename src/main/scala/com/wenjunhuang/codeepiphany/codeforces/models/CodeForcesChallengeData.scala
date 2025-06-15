package com.wenjunhuang.codeepiphany.codeforces.models

import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.TestCase

case class CodeForcesChallengeData(
  contestId: Long,
  index: String,
  description: String,
  supportedLanguages: Set[(Language, LanguageVersion)],
  testCases: List[TestCase]
)
