package com.wenjunhuang.codeepiphany.model

import cats.syntax.all.*
import org.typelevel.ci.CIString

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.ChallengeDifficulty.*

enum ChallengeDifficulty(val value: String) {
  case Easy     extends ChallengeDifficulty("easy")
  case Medium   extends ChallengeDifficulty("medium")
  case Hard     extends ChallengeDifficulty("hard")
  case Advanced extends ChallengeDifficulty("advanced")
  case Expert   extends ChallengeDifficulty("expert")

  def showAsHtml: String =
    this match
      case Easy =>
        s"<html><font color='${DIFFICULTY_EASY_COLOR}'>${Easy.show}</font></html>"
      case Medium =>
        s"<html><font color='${DIFFICULTY_MEDIUM_COLOR}'>${Medium.show}</font></html>"
      case Hard =>
        s"<html><font color='${DIFFICULTY_HARD_COLOR}'>${Hard.show}</font></html>"
      case Advanced =>
        s"<html><font color='${DIFFICULTY_ADVANCED_COLOR}'>${Advanced.show}</font></html>"
      case Expert =>
        s"<html><font color='${DIFFICULTY_EXPERT_COLOR}'>${Expert.show}</font></html>"
}

object ChallengeDifficulty {
  implicit val showInstance: cats.Show[ChallengeDifficulty] =
    cats.Show.show {
      case Easy     => PluginBundle.message("challenge.difficulty.easy")
      case Medium   => PluginBundle.message("challenge.difficulty.medium")
      case Hard     => PluginBundle.message("challenge.difficulty.hard")
      case Advanced => PluginBundle.message("challenge.difficulty.advanced")
      case Expert   => PluginBundle.message("challenge.difficulty.expert")
    }

  def fromCIString(str: CIString): Option[ChallengeDifficulty] =
    if str == CIString(Easy.value) then Some(Easy)
    else if str == CIString(Medium.value) then Some(Medium)
    else if str == CIString(Hard.value) then Some(Hard)
    else if str == CIString(Advanced.value) then Some(Advanced)
    else if str == CIString(Expert.value) then Some(Expert)
    else None

  // language=CSS
  val DIFFICULTY_EASY_COLOR     = "#1ab8a3"
  val DIFFICULTY_MEDIUM_COLOR   = "#ffc01e"
  val DIFFICULTY_HARD_COLOR     = "#ff375f"
  val DIFFICULTY_ADVANCED_COLOR = "#ff5164"
  val DIFFICULTY_EXPERT_COLOR   = "#ff4f64"
}
