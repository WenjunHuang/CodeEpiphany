package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.actions.LanguageParameterAction.*
import com.wenjunhuang.codeepiphany.model.{ Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.utils.actions.{ ParameterComboBoxAction, ParameterProvider }

class LanguageParameterAction
    extends ParameterComboBoxAction[Language, LanguageParameterProvider](
      LANGUAGE_PROVIDER_KEY,
      item => s"${item.show}",
      item => Option(item.show),
      item => Option(item.icon)
    ) {}

object LanguageParameterAction {
  val LANGUAGE_PROVIDER_KEY: DataKey[LanguageParameterProvider] =
    DataKey.create[LanguageParameterProvider]("LANGUAGE_PROVIDER_KEY")

  trait LanguageParameterProvider extends ParameterProvider[Language]
}
