package com.wenjunhuang.codeepiphany.hackerrank.actions

import cats.syntax.all.*

import com.intellij.openapi.actionSystem.*

import com.wenjunhuang.codeepiphany.hackerrank.actions.SkillParameterAction.*
import com.wenjunhuang.codeepiphany.hackerrank.model.HackerRankChallengeSkill
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class SkillParameterAction
    extends ParameterComboBoxAction[HackerRankChallengeSkill, SkillParameterProvider](
      SKILL_PROVIDER_KEY,
      item => item.show,
      item => Option(item.show),
      item => None
    ) {}

object SkillParameterAction {
  val SKILL_PROVIDER_KEY = DataKey.create[SkillParameterProvider]("SKILL_PROVIDER_KEY")
  trait SkillParameterProvider extends ParameterProvider[HackerRankChallengeSkill] {}
}
