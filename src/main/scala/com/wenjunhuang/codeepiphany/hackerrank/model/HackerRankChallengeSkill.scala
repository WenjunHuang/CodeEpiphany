package com.wenjunhuang.codeepiphany.hackerrank.model

import cats.Show
import org.typelevel.ci.CIString

import com.wenjunhuang.codeepiphany.PluginBundle

enum HackerRankChallengeSkill(val value: String) {
  case Intermediate extends HackerRankChallengeSkill("Problem Solving (Intermediate)")
  case Advanced extends HackerRankChallengeSkill("Problem Solving (Advanced)")
  case Basic extends HackerRankChallengeSkill("Problem Solving (Basic)")
}

object HackerRankChallengeSkill {
  implicit val showInstance: Show[HackerRankChallengeSkill] = Show.show[HackerRankChallengeSkill]{
    case Intermediate => PluginBundle.message("hackerrank.model.skill.intermediate")
    case Advanced => PluginBundle.message("hackerrank.model.skill.advanced")
    case Basic => PluginBundle.message("hackerrank.model.skill.basic")
  }

  def fromCIString(str: CIString): Option[HackerRankChallengeSkill] =
    if str == CIString(Intermediate.value) then Some(Intermediate)
    else if str == CIString(Advanced.value) then Some(Advanced)
    else if str == CIString(Basic.value) then Some(Basic)
    else None
}
