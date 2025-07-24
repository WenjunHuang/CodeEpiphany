package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredCodec

case class LeetCodeCategoryListItem(title: String, url: String, slug: String) derives ConfiguredCodec
