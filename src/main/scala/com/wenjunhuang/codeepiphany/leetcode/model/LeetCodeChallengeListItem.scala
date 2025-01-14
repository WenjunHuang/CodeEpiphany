package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.*
import io.circe.derivation.*

case class LeetCodeChallengeList(questions: List[LeetCodeChallengeListItem], total: Int)
    derives ConfiguredDecoder,
      ConfiguredEncoder

case class LeetCodeChallengeListItem(
  acRate: Double,
  difficulty: String,
  freqBar: Double,
  paidOnly: Boolean,
  status: Option[String] = None,
  frontendQuestionId: String,
  title: String,
  titleCn: Option[String] = None
) derives ConfiguredDecoder,
      ConfiguredEncoder
