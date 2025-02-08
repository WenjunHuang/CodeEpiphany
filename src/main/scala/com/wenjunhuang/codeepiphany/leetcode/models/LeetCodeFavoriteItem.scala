package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.*
import io.circe.derivation.ConfiguredDecoder

case class LeetCodeFavoriteItem(id: String, name: String, `type`: String) derives ConfiguredDecoder
