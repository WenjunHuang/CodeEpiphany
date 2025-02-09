package com.wenjunhuang.codeepiphany.model

import cats.syntax.all.*
import org.typelevel.ci.CIString

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.DifficultyColors.*
import ChallengeDifficulty.*

enum ChallengeDifficulty(val value: String) {
  case Easy     extends ChallengeDifficulty("easy")
  case Medium   extends ChallengeDifficulty("medium")
  case Hard     extends ChallengeDifficulty("hard")
  case Advanced extends ChallengeDifficulty("advanced")
  case Expert   extends ChallengeDifficulty("expert")
  case CodeDojoDefined(codeDojo: CodeDojo, codeDojoValue: String)
      extends ChallengeDifficulty(encodeCodeDojoDefined(codeDojo, codeDojoValue))

  def showAsHtml: String =
    this match
      case Easy =>
        s"<html><font color='${DIFFICULTY_EASY_COLOR}'>${this.show}</font></html>"
      case Medium =>
        s"<html><font color='${DIFFICULTY_MEDIUM_COLOR}'>${this.show}</font></html>"
      case Hard =>
        s"<html><font color='${DIFFICULTY_HARD_COLOR}'>${this.show}</font></html>"
      case Advanced =>
        s"<html><font color='${DIFFICULTY_ADVANCED_COLOR}'>${this.show}</font></html>"
      case Expert =>
        s"<html><font color='${DIFFICULTY_EXPERT_COLOR}'>${this.show}</font></html>"
      case CodeDojoDefined(codeDojo, codeDojoValue) =>
        codeDojo.difficultyShowAsHtml(codeDojoValue)
}

object ChallengeDifficulty {
  implicit val showInstance: cats.Show[ChallengeDifficulty] =
    cats.Show.show {
      case Easy     => PluginBundle.message("challenge.difficulty.easy")
      case Medium   => PluginBundle.message("challenge.difficulty.medium")
      case Hard     => PluginBundle.message("challenge.difficulty.hard")
      case Advanced => PluginBundle.message("challenge.difficulty.advanced")
      case Expert   => PluginBundle.message("challenge.difficulty.expert")
      case CodeDojoDefined(codeDojo, codeDojoValue) =>
        Option(PluginBundle.messageOfBuildKey(s"challenge.difficulty.${codeDojo.value}.${codeDojoValue}"))
          .getOrElse(codeDojo.difficultyShow(codeDojoValue))
    }

  def fromCIString(str: CIString): Option[ChallengeDifficulty] =
    if str == CIString(Easy.value) then Some(Easy)
    else if str == CIString(Medium.value) then Some(Medium)
    else if str == CIString(Hard.value) then Some(Hard)
    else if str == CIString(Advanced.value) then Some(Advanced)
    else if str == CIString(Expert.value) then Some(Expert)
    else decodeCodeDojoDefined(str.toString).map(CodeDojoDefined.apply.tupled)

  def encodeCodeDojoDefined(codeDojo: CodeDojo, codeDojoValue: String): String =
    s"${codeDojo.value}#$codeDojoValue"

  def decodeCodeDojoDefined(str: String): Option[(CodeDojo, String)] =
    str.split("#").toList match
      case codeDojo :: codeDojoValue :: Nil =>
        CodeDojo.fromCIString(CIString(codeDojo)).map((_, codeDojoValue))
      case _ => None

}

object DifficultyColors {
  // language=CSS
  val DIFFICULTY_EASY_COLOR     = "#4CAF50"
  val DIFFICULTY_MEDIUM_COLOR   = "#FFC107"
  val DIFFICULTY_HARD_COLOR     = "#F44336"
  val DIFFICULTY_ADVANCED_COLOR = "#7C4DFF"
  val DIFFICULTY_EXPERT_COLOR   = "#FF6D00"
}
