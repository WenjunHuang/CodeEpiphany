package com.wenjunhuang.codeepiphany.leetcode.actions

import cats.syntax.all.*

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.leetcode.actions.PositionParameterAction.{POSITION_PROVIDER_KEY, PositionParameterProvider}
import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeProblemsetPositionTag
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class PositionParameterAction
    extends ParameterComboBoxAction[LeetCodeProblemsetPositionTag, PositionParameterProvider](
      POSITION_PROVIDER_KEY,
      item => item.nameTranslated.filter(_.nonEmpty).getOrElse(item.name),
      item => item.nameTranslated.filter(_.nonEmpty).getOrElse(item.name).some,
      item => None
    ) {}

object PositionParameterAction {
  val POSITION_PROVIDER_KEY: DataKey[PositionParameterProvider] =
    DataKey.create[PositionParameterProvider]("POSITION_PROVIDER_KEY")
  trait PositionParameterProvider extends ParameterProvider[LeetCodeProblemsetPositionTag] {}
}
