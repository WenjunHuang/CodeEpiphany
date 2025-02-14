package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.{ ConfiguredDecoder, ConfiguredEncoder }

case class LeetCodeCompanyChallengeList(questions: List[LeetCodeCompanyChallengeListItem], total: Int)
    derives ConfiguredDecoder,
      ConfiguredEncoder

case class LeetCodeCompanyChallengeListItem(
  acRate: Double,
  difficulty: String,
  freqBar: Option[Double] = None,
  paidOnly: Boolean,
  solutionNum: Int = 0,
  status: Option[String] = None,
  questionFrontendId: String,
  title: String,
  titleCn: Option[String] = None,
  titleSlug: String
) derives ConfiguredDecoder,
      ConfiguredEncoder {
  def frontendQuestionId: String = questionFrontendId
}
