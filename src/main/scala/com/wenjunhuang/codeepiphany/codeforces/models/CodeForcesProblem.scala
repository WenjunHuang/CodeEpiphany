package com.wenjunhuang.codeepiphany.codeforces.models

import io.circe.derivation.ConfiguredDecoder

case class CodeForcesProblem(
  contestId: Option[Long],
  problemsetName: Option[String],
  index: String,
  name: String,
  `type`: String,
  points: Option[Float],
  rating: Option[Int],
  tags: List[String]
) derives ConfiguredDecoder

case class CodeForcesProblemStatistics(contestId: Option[Long], index: String, solvedCount: Int)
    derives ConfiguredDecoder

case class CodeForcesProblemResult(
  problems: List[CodeForcesProblem],
  problemStatistics: List[CodeForcesProblemStatistics]
) derives ConfiguredDecoder

case class CodeForcesProblemResponse(result: CodeForcesProblemResult) derives ConfiguredDecoder
