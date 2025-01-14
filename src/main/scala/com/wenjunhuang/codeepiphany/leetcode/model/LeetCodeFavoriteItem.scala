package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.derivation.ConfiguredDecoder
import io.circe.*

case class LeetCodeFavoriteItem(id: String, name: String, `type`: String) derives ConfiguredDecoder
