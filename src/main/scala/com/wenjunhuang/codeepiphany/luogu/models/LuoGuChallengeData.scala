package com.wenjunhuang.codeepiphany.luogu.models

import com.wenjunhuang.codeepiphany.model.{ Language, LanguageVersion }

case class LuoGuChallengeData(
  pid: String,
  title: String,
  description: String,
  supportedLanguages: Set[(Language, LanguageVersion)]
)
