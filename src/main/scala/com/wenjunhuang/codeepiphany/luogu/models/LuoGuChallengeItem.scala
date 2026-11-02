package com.wenjunhuang.codeepiphany.luogu.models

import io.circe.derivation.ConfiguredDecoder

case class LuoGuChallengeItem(
  pid: String,
  name: String,
  difficulty: LuoGuDifficulty,
  `type`: LuoGuQuestionBank,
  totalSubmit: Int,
  totalAccepted: Int,
  flag: Int,
  accepted: Boolean = false,
  submitted: Boolean = false
) derives ConfiguredDecoder
