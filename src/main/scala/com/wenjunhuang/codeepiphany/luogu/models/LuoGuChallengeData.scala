package com.wenjunhuang.codeepiphany.luogu.models

import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

case class LuoGuChallengeData(
  pid: String,
  title: String,
  description: String,
  supportedLanguages: Set[(Language, LanguageVersion)],
  testCases:List[ChallengeSettings.TestCase]
)
