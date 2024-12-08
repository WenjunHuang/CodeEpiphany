package com.wenjunhuang.codeepiphany.hackerrank

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.Language
import io.circe.Decoder

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

  case class ChallengeDomain(name:String,slug:String,subDomains:List[ChallengeSubdomain])
  case class ChallengeSubdomain(name: String, slug: String)

  case class ChallengeListItem(slug: String, difficultyName: String, successRatio: Double, name: String)
  object ChallengeListItem {
    given decoder: Decoder[ChallengeListItem] = Decoder.forProduct4("slug", "difficulty_name", "success_ratio", "name")(ChallengeListItem.apply)
  }
}
