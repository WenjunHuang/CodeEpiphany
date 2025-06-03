package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.actions

import cats.syntax.all.*

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeQuestionSolutionArticlesOrderBy
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.actions.ArticleOrderByAction.*
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}

class ArticleOrderByAction
    extends ParameterComboBoxAction[LeetCodeQuestionSolutionArticlesOrderBy, ArticleOrderByParameterProvider](
      LEETCODE_ARTICLE_ORDERBY_KEY,
      item => item.show,
      item => Some(item.show),
      item => None
    ) {}
object ArticleOrderByAction {
  final val LEETCODE_ARTICLE_ORDERBY_KEY =
    DataKey.create[ArticleOrderByParameterProvider]("LEETCODE_ARTICLE_ORDERBY_KEY")
  trait ArticleOrderByParameterProvider extends ParameterProvider[LeetCodeQuestionSolutionArticlesOrderBy] {}
}
