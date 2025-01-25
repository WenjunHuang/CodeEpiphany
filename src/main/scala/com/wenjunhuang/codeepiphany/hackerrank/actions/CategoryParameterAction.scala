package com.wenjunhuang.codeepiphany.hackerrank.actions

import com.intellij.openapi.actionSystem.*

import com.wenjunhuang.codeepiphany.hackerrank.actions.CategoryParameterAction.*
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class CategoryParameterAction
    extends ParameterComboBoxAction[Category, CategoryProvider](
      CATEGORY_PROVIDER_KEY,
      item => item.name,
      item => Option(item.value),
      item => None
    ) {}
object CategoryParameterAction {
  final val CATEGORY_PROVIDER_KEY = DataKey.create[CategoryProvider]("CATEGORY_PROVIDER_KEY")

  case class Category(name: String, value: String, marker: Any = null)
  trait CategoryProvider extends ParameterProvider[Category] {}
}
