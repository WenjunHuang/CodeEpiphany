package com.wenjunhuang.codeepiphany.hackerrank

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.Language
import io.circe.Decoder
import io.circe.derivation.{ Configuration, ConfiguredDecoder }
import io.circe.generic.auto.*

package object model {
  case class QuestionContent(slug: String, description: String, codeTemplate: String, language: Language)

  enum ChallengeStatus(val value: String) {
    case Solved   extends ChallengeStatus("solved")
    case Unsolved extends ChallengeStatus("unsolved")

    def show: String = PluginBundle.message(s"hackerrank.model.question.status.${this.toString}")
  }

  enum ChallengeSkill(val value: String) {
    case Intermediate extends ChallengeSkill("Problem Solving (Intermediate)")
    case Advanced     extends ChallengeSkill("Problem Solving (Advanced)")
    case Basic        extends ChallengeSkill("Problem Solving (Basic)")

    def show: String = PluginBundle.message(s"hackerrank.model.question.skill.${this.toString}")
  }

  enum ChallengeDifficulty(val value: String) {
    case Easy   extends ChallengeDifficulty("easy")
    case Medium extends ChallengeDifficulty("medium")
    case Hard   extends ChallengeDifficulty("hard")

    def show: String = PluginBundle.message(s"hackerrank.model.question.difficulty.${this.toString}")
  }

  private given hackerRankConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
  case class UserInfo(username: String, name: String, avatar: String) derives ConfiguredDecoder

  case class ChallengeDomain(name: String, slug: String, subDomains: List[ChallengeSubdomain])

  case class ChallengeSubdomain(name: String, slug: String)

  case class ChallengeListItem(
      id: Int,
      slug: String,
      name: String,
      bookmarked: Option[Boolean],
      solved: Option[Boolean],
      attempted: Option[Boolean],
      contestSlug: String,
      userScore: Double,
      preview: String,
      difficulty: Double,
      difficultyName: String,
      solvedScore: Double,
      successRatio: Double,
      totalCount: Int,
      solvedCount: Int
  ) derives ConfiguredDecoder

}
