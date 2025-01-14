package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.derivation.ConfiguredDecoder

case class LeetCodeTagTypeWithTags(name: String, transName: Option[String]) derives ConfiguredDecoder

case class LeetCodeTagRelation(questionNum: Int, tag: LeetCodeTag) derives ConfiguredDecoder
