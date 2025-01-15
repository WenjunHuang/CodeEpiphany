package com.wenjunhuang.codeepiphany.hackerrank

import cats.syntax.all.*
import io.circe.Decoder
import io.circe.derivation.{ Configuration, ConfiguredDecoder }

package object model {
  given hackerRankConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  final val EMPTY_USERINFO: UserInfo = UserInfo("", "", "")

  // constant for project euler contest
  // project euler contest seems to be a special case, so we need to define it here
  final val PROJECT_EULER_DOMAIN: ChallengeDomain =
    ChallengeDomain(99, "Project Euler", "projecteuler", Contest.ProjectEuler, List.empty)


}
