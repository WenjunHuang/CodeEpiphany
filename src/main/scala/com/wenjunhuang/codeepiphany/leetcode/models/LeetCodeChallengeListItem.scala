package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.*
import io.circe.derivation.*

case class LeetCodeChallengeList(questions: List[LeetCodeChallengeListItem], total: Int)
    derives ConfiguredDecoder,
      ConfiguredEncoder

case class LeetCodeChallengeListItem(
  acRate: Double,
  difficulty: String,
  freqBar: Option[Double] = None,
  paidOnly: Boolean,
  solutionNum:Int,
  status: Option[String] = None,
  frontendQuestionId: String,
  title: String,
  titleCn: Option[String] = None,
  titleSlug:String
) derives ConfiguredDecoder,
      ConfiguredEncoder
