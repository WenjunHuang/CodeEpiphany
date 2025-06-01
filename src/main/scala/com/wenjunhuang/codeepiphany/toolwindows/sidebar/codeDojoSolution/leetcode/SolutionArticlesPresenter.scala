package com.wenjunhuang.codeepiphany.toolwindows.sidebar.codeDojoSolution.leetcode

import cats.effect.IO

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.services.{ParametersQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.codeDojoSolution.leetcode.SolutionArticlesPresenter.*
import SolutionArticlesPresenter.*
import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeSolutionArticle
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

class SolutionArticlesPresenter(
  project: Project,
  questionSlug: String,
  private val myCodeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
) extends ParametersQueryPresenter[String, QueryParams, String](project, questionSlug) {

  override protected def prepareProviders(
    getter: () => QueryContext[QueryParams],
    updater: (QueryContext[QueryParams] => QueryContext[QueryParams]) => Unit,
    dataSink: DataSink
  ): ActionGroup = ???

  override protected def createQueryParametersTags(
    context: QueryContext[QueryParams],
    onCloseUpdater: (QueryContext[QueryParams] => QueryContext[QueryParams]) => Unit
  ): List[TagPaneAction] = ???

  override protected def createInitialQueryParameters(questionSlug: String): QueryContext[QueryParams] = ???

  override protected def executeQuery(context: QueryContext[QueryParams]): IO[(Pagination, List[String])] = ???

}

object SolutionArticlesPresenter {
  case class QueryParams(questionSlug: String,userInput:String,
                         orderBy:LeetCodeSolutionArticle
                        )

}
