package com.wenjunhuang.codeepiphany.luogu.ui

import cats.effect.IO
import javax.swing.{Icon, JTable, SwingConstants}
import javax.swing.table.{DefaultTableCellRenderer, TableCellRenderer}
import monocle.syntax.all.*
import org.jooq.impl.DSL
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.table.IconTableCellRenderer
import com.intellij.util.ui.ColorIcon

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.luogu.models.{LuoGuChallengeItem, LuoGuDifficulty, LuoGuSearchOrderBy, LuoGuTag}
import com.wenjunhuang.codeepiphany.luogu.ui.LuoGuParametersQueryPresenter.DIFFICULTY_TAG_RADIUS
import com.wenjunhuang.codeepiphany.model.{Actions, OrderDirection}
import com.wenjunhuang.codeepiphany.services.{ChallengeRepository, ParametersQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

class LuoGuParametersQueryPresenter(project: Project, bootstrap: LuoGuBootstrapParameters)
    extends ParametersQueryPresenter[
      LuoGuBootstrapParameters,
      LuoGuParametersQueryPresenter.QueryParams,
      LuoGuChallengeItem
    ](project, bootstrap) {

  override protected def prepareProviders(
    getter: () => QueryContext[LuoGuParametersQueryPresenter.QueryParams],
    updater: (
      QueryContext[LuoGuParametersQueryPresenter.QueryParams] => QueryContext[LuoGuParametersQueryPresenter.QueryParams]
    ) => Unit,
    dataSink: DataSink
  ): ActionGroup = {

    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createLuoGuChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel)
    )
    ActionManager.getInstance().getAction(Actions.ATCODER_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  }

  override protected def createQueryParametersTags(
    context: QueryContext[LuoGuParametersQueryPresenter.QueryParams],
    onCloseUpdater: (
      QueryContext[LuoGuParametersQueryPresenter.QueryParams] => QueryContext[LuoGuParametersQueryPresenter.QueryParams]
    ) => Unit
  ): List[TagPaneAction] = {
    context.criteria.selectedDifficulty.map { difficulty =>
      TagPaneAction(
        difficulty.toString,
        difficulty.showAsHtml,
        None,
        DIFFICULTY_TAG_RADIUS,
        None,
        Some(() => onCloseUpdater(_.focus(_.criteria.selectedDifficulty).modify(_ => None)))
      )
    }.toList
  }

  override protected def createInitialQueryParameters(
    boostrapParameters: LuoGuBootstrapParameters
  ): QueryContext[LuoGuParametersQueryPresenter.QueryParams] =
    QueryContext[LuoGuParametersQueryPresenter.QueryParams](
      LuoGuParametersQueryPresenter.QueryParams(None, Nil, None),
      Pagination()
    )

  override protected def executeQuery(
    context: QueryContext[LuoGuParametersQueryPresenter.QueryParams]
  ): IO[(Pagination, List[LuoGuChallengeItem])] = ???

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

      override def getPreferredStringValue: String = StringUtil.repeat("W", 20)

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
    },
    new OrderByColumnInfo[LuoGuChallengeItem, String]("Acceptance") {
      override def valueOf(item: LuoGuChallengeItem): String =
        f"${(item.totalAccepted.toDouble / item.totalSubmit.toDouble) * 10}%.2f%%"

      override def getRenderer(item: LuoGuChallengeItem): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(LuoGuSearchOrderBy.ACRate)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(LuoGuSearchOrderBy.ACRate, filter)
    }
  )

}

object LuoGuParametersQueryPresenter {
  case class QueryParams(
    selectedDifficulty: Option[LuoGuDifficulty],
    selectedTags: List[LuoGuTag],
    orderBy: Option[(LuoGuSearchOrderBy, OrderDirection)]
  )

  private val DIFFICULTY_TAG_RADIUS = 0.3f
}
