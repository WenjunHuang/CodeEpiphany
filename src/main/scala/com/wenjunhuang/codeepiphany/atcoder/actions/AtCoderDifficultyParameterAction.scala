package com.wenjunhuang.codeepiphany.atcoder.actions

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderDifficultyParameterAction.*
import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderDifficulty
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class AtCoderDifficultyParameterAction
    extends ParameterComboBoxAction[AtCoderDifficulty, AtCoderDifficultyParameterProvider](
      ATCODER_DIFFICULTIES_PROVIDER_KEY,
      item => item.showAsHtml,
      item => Option(item.toString),
      item => None
    ) {}
object AtCoderDifficultyParameterAction {
  final val ATCODER_DIFFICULTIES_PROVIDER_KEY =
    DataKey.create[AtCoderDifficultyParameterProvider]("ATCODER_DIFFICULTIES_PROVIDER_KEY")
  trait AtCoderDifficultyParameterProvider extends ParameterProvider[AtCoderDifficulty] {}
}
