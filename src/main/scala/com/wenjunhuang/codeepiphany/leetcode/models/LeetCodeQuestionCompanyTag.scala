package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredDecoder

case class LeetCodeQuestionCompanyTag(name: String, slug: String, id: String, questionCount: Int)
    derives ConfiguredDecoder
