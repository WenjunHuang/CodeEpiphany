package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.derivation.ConfiguredDecoder

case class LeetCodeTag(
  id: Option[String] = None,
  name: String,
  nameTranslated: Option[String] = None,
  slug: String,
  questions: List[Int] = Nil
) derives ConfiguredDecoder
