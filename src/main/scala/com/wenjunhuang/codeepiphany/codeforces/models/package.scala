package com.wenjunhuang.codeepiphany.codeforces

import io.circe.derivation.Configuration

import com.wenjunhuang.codeepiphany.model.ChallengeDifficulty

package object models {
  given Configuration = Configuration.default.withDefaults

  def codeForcesRatingToDifficulty(rating: Option[Int]): ChallengeDifficulty =
    rating match
      case Some(r) if r < 1200  => ChallengeDifficulty.Easy
      case Some(r) if r < 1600  => ChallengeDifficulty.Medium
      case Some(r) if r < 1900  => ChallengeDifficulty.Hard
      case Some(r) if r < 2200  => ChallengeDifficulty.Advanced
      case Some(r) if r >= 2200 => ChallengeDifficulty.Expert
      case _                    => ChallengeDifficulty.Easy
  def codeForcesDifficultyToRatingRange(difficulty: ChallengeDifficulty): (Int, Int) =
    difficulty match
      case ChallengeDifficulty.Easy     => (0, 1199)
      case ChallengeDifficulty.Medium   => (1200, 1599)
      case ChallengeDifficulty.Hard     => (1600, 1899)
      case ChallengeDifficulty.Advanced => (1900, 2199)
      case ChallengeDifficulty.Expert   => (2200, Int.MaxValue)
      case ChallengeDifficulty.CodeDojoDefined(_, _) => (0, Int.MaxValue)
}
