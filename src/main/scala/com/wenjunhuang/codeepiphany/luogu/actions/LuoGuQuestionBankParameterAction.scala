package com.wenjunhuang.codeepiphany.luogu.actions

import cats.syntax.all.*
import com.intellij.openapi.actionSystem.DataKey
import com.wenjunhuang.codeepiphany.luogu.actions.LuoGuQuestionBankParameterAction.*
import com.wenjunhuang.codeepiphany.luogu.models.LuoGuQuestionBank
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class LuoGuQuestionBankParameterAction
    extends ParameterComboBoxAction[LuoGuQuestionBank, LuoGuQuestionBankParameterProvider](
      LUOGU_QUESTION_BANK_PROVIDER_KEY,
      item => item.show,
      item => Option(item.show),
      item => None
    ) {}

object LuoGuQuestionBankParameterAction {

  final val LUOGU_QUESTION_BANK_PROVIDER_KEY =
    DataKey.create[LuoGuQuestionBankParameterProvider]("LUOGU_QUESTION_BANK_PROVIDER_KEY")

  trait LuoGuQuestionBankParameterProvider extends ParameterProvider[LuoGuQuestionBank] {}
}
