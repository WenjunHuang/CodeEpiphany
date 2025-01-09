package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.DataKey
import com.wenjunhuang.codeepiphany.actions.LanguageParameterAction.*
import com.wenjunhuang.codeepiphany.model.{ Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class LanguageParameterAction
    extends ParameterComboBoxAction[(Language, LanguageVersion), LanguageParameterProvider](
      LANGUAGE_PROVIDER_KEY,
      item => s"${item._1.value}${item._2.version}",
      item => Option(item._1.show),
      item => Option(item._1.icon)
    ) {}

object LanguageParameterAction {
  val LANGUAGE_PROVIDER_KEY: DataKey[LanguageParameterProvider] =
    DataKey.create[LanguageParameterProvider]("LANGUAGE_PROVIDER_KEY")

  trait LanguageParameterProvider extends ParameterProvider[(Language, LanguageVersion)]
}
