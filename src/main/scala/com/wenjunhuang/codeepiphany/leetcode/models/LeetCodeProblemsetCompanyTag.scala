package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredDecoder

case class LeetCodeProblemsetCompanyTag(name: String, slug: String) derives ConfiguredDecoder
