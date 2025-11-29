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
  solutionNum: Option[Int] = None,
  status: Option[String] = None,
  frontendQuestionId: String,
  title: String,
  titleCn: Option[String] = None,
  titleSlug: String
) derives ConfiguredDecoder,
      ConfiguredEncoder

case class LeetCodeChallengeListV2(questions: List[LeetCodeChallengeListItemV2], totalLength: Int)
    derives ConfiguredDecoder,
      ConfiguredEncoder {
  def toV1: LeetCodeChallengeList = LeetCodeChallengeList(questions = questions.map(_.toV1), total = totalLength)
}

case class LeetCodeChallengeListItemV2(
  acRate: Double,
  difficulty: String,
  frequency: Option[Double] = None,
  paidOnly: Boolean,
  status: Option[String] = None,
  questionFrontendId: String,
  title: String,
  titleSlug: String
) derives ConfiguredDecoder,
      ConfiguredEncoder {
  def toV1: LeetCodeChallengeListItem = LeetCodeChallengeListItem(
    acRate,
    difficulty = difficulty,
    freqBar = frequency,
    paidOnly = paidOnly,
    solutionNum = None,
    status = status,
    frontendQuestionId = questionFrontendId,
    title = title,
    titleCn = None,
    titleSlug = titleSlug
  )
}
