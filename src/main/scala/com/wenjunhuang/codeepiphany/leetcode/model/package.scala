package com.wenjunhuang.codeepiphany.leetcode

import io.circe.derivation.Configuration
import org.typelevel.ci.CIString

import com.wenjunhuang.codeepiphany.model.*

package object model {
  given Configuration = Configuration.default.withDefaults

  extension (codeDojo: CodeDojo) {
    def leetCodeDifficulty(difficulty: ChallengeDifficulty): String =
      difficulty match
        case ChallengeDifficulty.Easy     => "EASY"
        case ChallengeDifficulty.Medium   => "MEDIUM"
        case ChallengeDifficulty.Hard     => "HARD"
        case ChallengeDifficulty.Advanced => "HARD"
        case ChallengeDifficulty.Expert   => "HARD"

    def fromLeetCodeDifficulty(difficulty: String): ChallengeDifficulty =
      CIString(difficulty) match
        case d if d == CIString("easy")   => ChallengeDifficulty.Easy
        case m if m == CIString("medium") => ChallengeDifficulty.Medium
        case h if h == CIString("hard")   => ChallengeDifficulty.Hard

    def leetCodeStatus(status: ChallengeStatus): String = status match
      case ChallengeStatus.Unsolved => "NOT_STARTED"
      case ChallengeStatus.Solved   => "AC"
      case ChallengeStatus.Tried    => "TRIED"

    def fromLeetCodeStatus(status: String): ChallengeStatus = CIString(status) match
      case ns if ns == CIString("NOT_STARTED") => ChallengeStatus.Unsolved
      case ac if ac == CIString("AC")          => ChallengeStatus.Solved
      case t if t == CIString("TRIED")         => ChallengeStatus.Tried

    def leetCodeOrderDirection(direction: OrderDirection): String = direction match
      case OrderDirection.Ascending  => "ASCENDING"
      case OrderDirection.Descending => "DESCENDING"

    def fromLeetCodeOrderDirection(direction: String): OrderDirection = CIString(direction) match
      case a if a == CIString("ASCENDING")  => OrderDirection.Ascending
      case b if b == CIString("DESCENDING") => OrderDirection.Descending
  }
}
