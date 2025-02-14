package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredDecoder

case class LeetCodeProblemsetPositionTag(name: String, nameTranslated: Option[String] = None, slug: String)
    derives ConfiguredDecoder
