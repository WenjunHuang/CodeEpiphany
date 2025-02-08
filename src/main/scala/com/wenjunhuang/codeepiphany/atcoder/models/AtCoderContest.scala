package com.wenjunhuang.codeepiphany.atcoder.models

import io.circe.{ Decoder, HCursor, Json }
import io.circe.derivation.ConfiguredDecoder

case class AtCoderContest(id: String, startEpochSecond: Long, durationSecond: Long, title: String, rateChange: String)
    derives ConfiguredDecoder
