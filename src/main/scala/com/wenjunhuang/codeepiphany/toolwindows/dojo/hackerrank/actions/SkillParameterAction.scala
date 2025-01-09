package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.actions

import cats.syntax.all.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.{ CheckboxAction, ComboBoxAction }

import javax.swing.JComponent
import SkillParameterAction.*
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}
import SkillParameterAction.*
import com.wenjunhuang.codeepiphany.hackerrank.model.HackerRankChallengeSkill

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
