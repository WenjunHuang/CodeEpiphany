package com.wenjunhuang.codeepiphany.hackerrank.ui

import com.wenjunhuang.codeepiphany.hackerrank.model.{HackerRankChallengeDomain, HackerRankUserInfo}

case class HackerRankBootstrapParameters(
  userInfo: HackerRankUserInfo,
  challengeDomains: List[HackerRankChallengeDomain]
)
