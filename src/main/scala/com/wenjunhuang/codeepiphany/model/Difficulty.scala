package com.wenjunhuang.codeepiphany.model

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.utils.Colors
import org.typelevel.ci.CIString
import cats.syntax.all.*
import com.intellij.openapi.util.text.StringUtil

enum Difficulty(val value: String) {
  case Easy     extends Difficulty("easy")
  case Medium   extends Difficulty("medium")
  case Hard     extends Difficulty("hard")
  case Advanced extends Difficulty("advanced")
  case Expert   extends Difficulty("expert")

  def showAsHtml: String =
    this match
      case Easy =>
        s"<html><font color='${Colors.DIFFICULTY_EASY_COLOR}'>${Easy.show}</font></html>"
      case Medium =>
        s"<html><font color='${Colors.DIFFICULTY_MEDIUM_COLOR}'>${Medium.show}</font></html>"
      case Hard =>
        s"<html><font color='${Colors.DIFFICULTY_HARD_COLOR}'>${Hard.show}</font></html>"
      case Advanced =>
        s"<html><font color='${Colors.DIFFICULTY_ADVANCED_COLOR}'>${Advanced.show}</font></html>"
      case Expert =>
        s"<html><font color='${Colors.DIFFICULTY_EXPERT_COLOR}'>${Expert.show}</font></html>"
}

object Difficulty {
  implicit val showInstance: cats.Show[Difficulty] =
    cats.Show.show(it => PluginBundle.message(s"challenge.difficulty.${StringUtil.decapitalize(it.toString)}"))

  def fromCIString(str: CIString): Option[Difficulty] =
    if str == CIString(Easy.value) then Some(Easy)
    else if str == CIString(Medium.value) then Some(Medium)
    else if str == CIString(Hard.value) then Some(Hard)
    else if str == CIString(Advanced.value) then Some(Advanced)
    else if str == CIString(Expert.value) then Some(Expert)
    else None
}
