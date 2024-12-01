package com.wenjunhuang.codeepiphany.hackerrank

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.Language
import io.circe.Decoder

package object model {
  case class QuestionContent(slug: String, description: String, codeTemplate: String, language: Language)

  enum QuestionStatus(val value: String) {
    case Solved   extends QuestionStatus("solved")
    case Unsolved extends QuestionStatus("unsolved")

    def show: String = PluginBundle.message(s"hackerrank.model.question.status.${this.toString}")
  }

  enum QuestionSkill(val value: String) {
    case Intermediate extends QuestionSkill("Problem Solving (Intermediate)")
    case Advanced     extends QuestionSkill("Problem Solving (Advanced)")
    case Basic        extends QuestionSkill("Problem Solving (Basic)")

    def show: String = PluginBundle.message(s"hackerrank.model.question.skill.${this.toString}")
  }

  enum QuestionDifficulty(val value: String) {
    case Easy   extends QuestionDifficulty("easy")
    case Medium extends QuestionDifficulty("medium")
    case Hard   extends QuestionDifficulty("hard")

    def show: String = PluginBundle.message(s"hackerrank.model.question.difficulty.${this.toString}")
  }

  enum QuestionSubdomain(val value: String) {
    case Warmup                 extends QuestionSubdomain("warmup")
    case Implementation         extends QuestionSubdomain("implementation")
    case Strings                extends QuestionSubdomain("strings")
    case Sorting                extends QuestionSubdomain("sorting")
    case Search                 extends QuestionSubdomain("search")
    case GraphTheory            extends QuestionSubdomain("graph-theory")
    case Greedy                 extends QuestionSubdomain("greedy")
    case DynamicProgramming     extends QuestionSubdomain("dynamic-programming")
    case ConstructiveAlgorithms extends QuestionSubdomain("constructive-algorithms")
    case BitManipulation        extends QuestionSubdomain("bit-manipulation")
    case Recursion              extends QuestionSubdomain("recursion")
    case GameTheory             extends QuestionSubdomain("game-theory")
    case NPComplete             extends QuestionSubdomain("np-complete-problems")
    case Debugging              extends QuestionSubdomain("debugging")

    def show: String = PluginBundle.message(s"hackerrank.model.question.subdomains.${this.toString}")
  }

  case class ChallengeListItem(slug: String, difficultyName: String, successRatio: Double, name: String)
  object ChallengeListItem {
    given decoder: Decoder[ChallengeListItem] = Decoder.forProduct4("slug", "difficulty_name", "success_ratio", "name")(ChallengeListItem.apply)
  }
}
