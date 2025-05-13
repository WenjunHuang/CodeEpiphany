package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredCodec

case class LeetCodeSolutionTag(name: String, nameTranslated: Option[String], slug: String) derives ConfiguredCodec

case class LeetCodeSolutionTags(languageTags: List[LeetCodeSolutionTag], knowledgeTags: List[LeetCodeSolutionTag])
    derives ConfiguredCodec
