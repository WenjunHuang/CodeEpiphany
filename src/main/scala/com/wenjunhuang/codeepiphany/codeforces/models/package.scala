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
}
