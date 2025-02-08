package com.wenjunhuang.codeepiphany.hackerrank.models

import io.circe.derivation.ConfiguredDecoder

case class HackerRankChallengeSearchByKeyWord(
  contestName: String,
  contestSlug: String,
  challengeId: Int,
  challengeName: String,
  challengeSlug: String,
  name: String
) derives ConfiguredDecoder
