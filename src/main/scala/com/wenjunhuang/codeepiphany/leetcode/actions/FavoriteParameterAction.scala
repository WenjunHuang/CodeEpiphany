package com.wenjunhuang.codeepiphany.leetcode.actions

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.leetcode.actions.FavoriteParameterAction.{FAVORITE_PROVIDER_KEY, FavoriteParameterProvider}
import com.wenjunhuang.codeepiphany.leetcode.model.LeetCodeFavoriteItem
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class FavoriteParameterAction
    extends ParameterComboBoxAction[LeetCodeFavoriteItem, FavoriteParameterProvider](
      FAVORITE_PROVIDER_KEY,
      item => item.name,
      item => Option(item.name),
      item => None
    ) {}

object FavoriteParameterAction {
  val FAVORITE_PROVIDER_KEY: DataKey[FavoriteParameterProvider] =
    DataKey.create[FavoriteParameterProvider]("FAVORITE_PROVIDER_KEY")
  trait FavoriteParameterProvider extends ParameterProvider[LeetCodeFavoriteItem] {}
}
