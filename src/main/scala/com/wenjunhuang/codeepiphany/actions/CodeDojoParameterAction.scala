package com.wenjunhuang.codeepiphany.actions

import cats.syntax.all.*
import com.intellij.openapi.actionSystem.DataKey
import com.wenjunhuang.codeepiphany.actions.CodeDojoParameterAction.*
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class CodeDojoParameterAction
    extends ParameterComboBoxAction[CodeDojo, CodeDojoParameterProvider](
      CODEDOJO_PROVIDER_KEY,
      item => item.show,
      item => Option(item.show),
      item => item.getIcon
    ) {}

object CodeDojoParameterAction {
  val CODEDOJO_PROVIDER_KEY: DataKey[CodeDojoParameterProvider] = DataKey.create[CodeDojoParameterProvider]("CODEDOJO_PROVIDER_KEY")
  trait CodeDojoParameterProvider extends ParameterProvider[CodeDojo]
}
