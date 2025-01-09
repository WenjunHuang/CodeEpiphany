package com.wenjunhuang.codeepiphany.actions

import cats.syntax.all.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.{ CheckboxAction, ComboBoxAction }
import com.wenjunhuang.codeepiphany.actions.StatusParameterAction.*
import com.wenjunhuang.codeepiphany.model.ChallengeStatus
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

import javax.swing.JComponent

class StatusParameterAction
    extends ParameterComboBoxAction[ChallengeStatus, StatusParameterProvider](
      STATUS_PROVIDER_KEY,
      item => item.show,
      item => Option(item.value),
      item => None
    ) {}

object StatusParameterAction {
  val STATUS_PROVIDER_KEY = DataKey.create[StatusParameterProvider]("STATUS_PROVIDER_KEY")

  trait StatusParameterProvider extends ParameterProvider[ChallengeStatus] {}
}
