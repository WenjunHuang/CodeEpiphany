package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.derivation.ConfiguredDecoder

case class LeetCodeTag(id: String, name: String, nameTranslated: Option[String] = None, slug: String)
    derives ConfiguredDecoder
