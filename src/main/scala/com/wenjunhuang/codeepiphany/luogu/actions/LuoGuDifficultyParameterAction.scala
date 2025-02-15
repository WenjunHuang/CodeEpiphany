package com.wenjunhuang.codeepiphany.luogu.actions

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.luogu.models.LuoGuDifficulty
import com.wenjunhuang.codeepiphany.utils.actions.{ ParameterComboBoxAction, ParameterProvider }
import LuoGuDifficultyParameterAction.*

class LuoGuDifficultyParameterAction
    extends ParameterComboBoxAction[LuoGuDifficulty, LuoGuDifficultyParameterProvider](
      LUOGU_DIFFICULTIES_PROVIDER_KEY,
      item => item.showAsHtml,
      item => Option(item.toString),
      item => None
    ) {}
object LuoGuDifficultyParameterAction {
  final val LUOGU_DIFFICULTIES_PROVIDER_KEY =
    DataKey.create[LuoGuDifficultyParameterProvider]("LUOGU_DIFFICULTIES_PROVIDER_KEY")
  trait LuoGuDifficultyParameterProvider extends ParameterProvider[LuoGuDifficulty] {}
}
