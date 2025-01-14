package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.*
import io.circe.derivation.ConfiguredDecoder

case class LeetCodeFavoriteItem(id: String, name: String, `type`: String) derives ConfiguredDecoder
