package com.wenjunhuang.codeepiphany.luogu.ui

import cats.effect.IO
import monocle.syntax.all.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup
import com.wenjunhuang.codeepiphany.luogu.models.{LuoGuChallengeItem, LuoGuSearchOrderBy}
import com.wenjunhuang.codeepiphany.luogu.ui.LuoGuKeywordQueryPresenter.*
import com.wenjunhuang.codeepiphany.model.OrderDirection
import com.wenjunhuang.codeepiphany.services.{KeywordQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.services.KeywordQueryPresenter.KeywordHolder
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink

class LuoGuKeywordQueryPresenter(project: Project, bootstrap: LuoGuBootstrapParameters)
    extends KeywordQueryPresenter[LuoGuBootstrapParameters, QueryParams, LuoGuChallengeItem](project, bootstrap) {
  override protected def createInitialQueryParameters(
    boostrapParameters: LuoGuBootstrapParameters
  ): QueryContext[QueryParams] = QueryContext[QueryParams](criteria = QueryParams("", None), pagination = Pagination())

  override protected def executeQuery(context: QueryContext[QueryParams]): IO[(Pagination, List[LuoGuChallengeItem])] =
    ???

  override protected def updateQueryUI(context: QueryContext[QueryParams]): Unit = {}

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    super.uiDataSnapshot(dataSink)

    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createLuoGuChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel)
    )
  }

  def getDirectionOf(field: LuoGuSearchOrderBy): Option[OrderDirection] =
    myQueryStateManager.get.criteria.orderBy.collect {
      case (f, d) if f == field => d
    }

  def setDirectionOf(field: LuoGuSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    direction match
      case None            => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(None))
      case Some(direction) => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(Some((field, direction))))
    requery(true)
  }

  override def getQueryResultColumns: Array[OrderByColumnInfo[LuoGuChallengeItem, ?]] = Array(
    new OrderByColumnInfo[LuoGuChallengeItem, String]("Id") {
      override def valueOf(item: LuoGuChallengeItem): String =
        item.pid

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)
      override def enableOrderBy: Boolean          = false
    },
    new OrderByColumnInfo[LuoGuChallengeItem, String]("Title") {
      override def valueOf(item: LuoGuChallengeItem): String = item.title

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)

      override def enableOrderBy: Boolean = false
    },
    new OrderByColumnInfo[LuoGuChallengeItem, String]("Difficulty") {
      override def valueOf(item: LuoGuChallengeItem): String =
        item.difficulty.showAsHtml

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(LuoGuSearchOrderBy.Difficulty)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(LuoGuSearchOrderBy.Difficulty, filter)
    }
  )
}

object LuoGuKeywordQueryPresenter {
  case class QueryParams(keyword: String, orderBy: Option[(LuoGuSearchOrderBy, OrderDirection)])
  implicit val keywordHolder: KeywordHolder[QueryParams] = new KeywordHolder[QueryParams] {
    override def keyword(v: QueryParams): String = v.keyword

    override def updateKeyword(v: QueryParams, keyword: String): QueryParams = v.copy(keyword = keyword)
  }
}
