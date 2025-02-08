package com.wenjunhuang.codeepiphany.atcoder.models

import io.circe.derivation.ConfiguredDecoder

case class AtCoderContest(
  id: String,
  startEpochSecond: Option[Long] = None,
  durationSecond: Option[Long] = None,
  title: String,
  rateChange: Option[String] = None
) derives ConfiguredDecoder
