package com.wenjunhuang.codeepiphany.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.{ CheckboxAction, ComboBoxAction }
import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.*
import com.wenjunhuang.codeepiphany.model.ChallengeDifficulty
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

import javax.swing.JComponent
class DifficultyParameterAction
    extends ParameterComboBoxAction[ChallengeDifficulty, DifficultyParameterProvider](
      DIFFICULTIES_PROVIDER_KEY,
      item => item.showAsHtml,
      item => Option(item.value),
      item => None
    ) {}
object DifficultyParameterAction {
  final val DIFFICULTIES_PROVIDER_KEY = DataKey.create[DifficultyParameterProvider]("DIFFICULTIES_PROVIDER_KEY")
  trait DifficultyParameterProvider extends ParameterProvider[ChallengeDifficulty] {}
}
