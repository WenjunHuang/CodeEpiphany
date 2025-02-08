package com.wenjunhuang.codeepiphany.hackerrank

import io.circe.derivation.Configuration

package object models {
  given Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  final val EMPTY_USERINFO: HackerRankUserInfo = HackerRankUserInfo("", "", "")

  // constant for project euler contest
  // project euler contest seems to be a special case, so we need to define it here
  final val PROJECT_EULER_DOMAIN: HackerRankChallengeDomain =
    HackerRankChallengeDomain(99, "Project Euler", "projecteuler", HackerRankContest.ProjectEuler, List.empty)


}
