package com.wenjunhuang.codeepiphany.leetcode.actions

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.leetcode.actions.CompanyParameterAction.{COMPANY_PROVIDER_KEY, CompanyParameterProvider}
import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeQuestionCompanyTag
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class CompanyParameterAction
    extends ParameterComboBoxAction[LeetCodeQuestionCompanyTag, CompanyParameterProvider](
      COMPANY_PROVIDER_KEY,
      item => s"<html><span>${item.name}</span> <font color='#ffa116'>${item.questionCount}</font></html>",
      item => Option(item.name),
      item => None
    ) {}

object CompanyParameterAction {
  val COMPANY_PROVIDER_KEY: DataKey[CompanyParameterProvider] =
    DataKey.create[CompanyParameterProvider]("COMPANY_PROVIDER_KEY")
  trait CompanyParameterProvider extends ParameterProvider[LeetCodeQuestionCompanyTag] {}
}
