package com.wenjunhuang.codeepiphany.atcoder.models

import io.circe.derivation.ConfiguredDecoder

case class AtCoderProblem(
  id: String,
  contestId: String,
  problemIndex: String,
  name: String,
  title: String,
  shortestSubmissionId: Option[Long] = None,
  shortestContestId: Option[String] = None,
  shortestUserId: Option[String] = None,
  fastestSubmissionId: Option[Long] = None,
  fastestContestId: Option[String] = None,
  fastestUserId: Option[String] = None,
  firstSubmissionId: Option[Long] = None,
  firstContestId: Option[String] = None,
  firstUserId: Option[String] = None,
  solverCount: Option[Int] = None
) derives ConfiguredDecoder
