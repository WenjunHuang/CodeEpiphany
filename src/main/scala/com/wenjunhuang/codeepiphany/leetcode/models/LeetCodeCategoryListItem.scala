package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredDecoder
import io.circe.generic.auto.*

case class LeetCodeCategoryListItem(title: String, url: String, slug: String) derives ConfiguredDecoder
