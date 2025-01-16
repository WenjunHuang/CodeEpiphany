package com.wenjunhuang.codeepiphany.leetcode.model

import io.circe.derivation.ConfiguredDecoder
import io.circe.generic.auto.*

case class LeetCodeUserInfo(
  isSignedIn: Boolean,
  isPremium: Option[Boolean] = None,
  username: Option[String] = None,
  realName: Option[String] = None,
  avatar: Option[String] = None,
  userSlug: Option[String] = None
) derives ConfiguredDecoder

object LeetCodeUserInfo {
  val EMPTY_USERINFO: LeetCodeUserInfo = LeetCodeUserInfo(isSignedIn = false)
}