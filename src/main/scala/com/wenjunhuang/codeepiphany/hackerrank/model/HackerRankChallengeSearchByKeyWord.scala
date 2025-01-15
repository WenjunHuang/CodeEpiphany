package com.wenjunhuang.codeepiphany.hackerrank.model

import io.circe.derivation.ConfiguredDecoder

case class ChallengeSearchByKeyWord(
  contestName: String,
  contestSlug: String,
  challengeId: Int,
  challengeName: String,
  challengeSlug: String,
  name: String
) derives ConfiguredDecoder
