package com.wenjunhuang.codeepiphany.leetcode.actions

import com.intellij.openapi.actionSystem.DataKey
import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeCategoryParameterAction.*
import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeCategoryListItem
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class LeetCodeCategoryParameterAction
    extends ParameterComboBoxAction[LeetCodeCategoryListItem, LeetCodeCategoryProvider](
      LeetCodeCategoryParameterAction.LEETCODE_CATEGORY_PROVIDER_KEY,
      item => item.title,
      item => Option(item.slug),
      item => None
    ) {}

object LeetCodeCategoryParameterAction {
  val LEETCODE_CATEGORY_PROVIDER_KEY: DataKey[LeetCodeCategoryProvider] =
    DataKey.create[LeetCodeCategoryProvider]("LEETCODE_CATEGORY_PROVIDER_KEY")
  trait LeetCodeCategoryProvider extends ParameterProvider[LeetCodeCategoryListItem] {}
}
