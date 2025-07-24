package com.wenjunhuang.codeepiphany.actions

import cats.syntax.all.*
import com.intellij.openapi.actionSystem.*
import com.wenjunhuang.codeepiphany.actions.StatusParameterAction.*
import com.wenjunhuang.codeepiphany.model.ChallengeStatus
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class StatusParameterAction
    extends ParameterComboBoxAction[ChallengeStatus, StatusParameterProvider](
      STATUS_PROVIDER_KEY,
      item => item.show,
      item => Option(item.value),
      item => None
    ) {}

object StatusParameterAction {
  val STATUS_PROVIDER_KEY: DataKey[StatusParameterProvider] = DataKey.create[StatusParameterProvider]("STATUS_PROVIDER_KEY")

  trait StatusParameterProvider extends ParameterProvider[ChallengeStatus] {}
}
