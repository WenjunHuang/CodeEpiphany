package com.wenjunhuang.codeepiphany.hackerrank.models

import io.circe.derivation.ConfiguredDecoder

case class HackerRankUserInfo(username: String, name: String, avatar: String) derives ConfiguredDecoder
