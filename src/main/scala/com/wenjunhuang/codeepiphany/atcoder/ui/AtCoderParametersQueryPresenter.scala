package com.wenjunhuang.codeepiphany.atcoder.ui

import cats.effect.IO
import javax.swing.{Icon, JTable}
import javax.swing.table.TableCellRenderer
import monocle.syntax.all.*
import org.jooq.impl.DSL
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.table.IconTableCellRenderer
import com.intellij.util.ui.ColorIcon

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup
import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderDifficultyParameterAction
import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderDifficultyParameterAction.AtCoderDifficultyParameterProvider
import com.wenjunhuang.codeepiphany.atcoder.models.{AtCoderDifficulty, AtCoderSearchOrderBy}
import com.wenjunhuang.codeepiphany.atcoder.ui.AtCoderParametersQueryPresenter.*
import com.wenjunhuang.codeepiphany.database.tables.records.AtcoderProblemsRecord
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.{Actions, OrderDirection}
import com.wenjunhuang.codeepiphany.services.{ChallengeRepository, ParametersQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

class AtCoderParametersQueryPresenter(project: Project, bootstrap: AtCoderBootstrapParameters)
    extends ParametersQueryPresenter[
      AtCoderBootstrapParameters,
      AtCoderParametersQueryPresenter.QueryParams,
      AtCoderTableItem
    ](project, bootstrap) {

  override protected def prepareProviders(
    getter: () => QueryContext[AtCoderParametersQueryPresenter.QueryParams],
    updater: (
      QueryContext[AtCoderParametersQueryPresenter.QueryParams] => QueryContext[
        AtCoderParametersQueryPresenter.QueryParams
      ]
    ) => Unit,
    dataSink: DataSink
  ): ActionGroup = {

    val difficultyParameterProvider = new AtCoderDifficultyParameterProvider {
      override def getAllItems: List[AtCoderDifficulty] = List(AtCoderDifficulty.values*)

      override def isMultipleSelection: Boolean = false

      override def isSelected(item: AtCoderDifficulty): Boolean = getter().criteria.selectedDifficulty.contains(item)

      override def getSelectedItems: List[AtCoderDifficulty] = getter().criteria.selectedDifficulty.toList

      override def addSelectedItems(items: List[AtCoderDifficulty]): Unit = updater(
        _.focus(_.criteria.selectedDifficulty).replace(items.headOption)
      )

      override def toggleSelection(item: AtCoderDifficulty): Unit = updater { old =>
        val newSelected = old.criteria.selectedDifficulty match
          case Some(selected) if selected == item => None
          case _                                  => Some(item)
        old.focus(_.criteria.selectedDifficulty).replace(newSelected)
      }

      override def removeSelectedItems(items: List[AtCoderDifficulty]): Unit = updater(
        _.focus(_.criteria.selectedDifficulty).replace(None)
      )
    }

    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createAtCoderChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel)
    )
    dataSink.set(AtCoderDifficultyParameterAction.ATCODER_DIFFICULTIES_PROVIDER_KEY, difficultyParameterProvider)
    ActionManager.getInstance().getAction(Actions.ATCODER_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  }

  override protected def createQueryParametersTags(
    context: QueryContext[AtCoderParametersQueryPresenter.QueryParams],
    onCloseUpdater: (
      QueryContext[AtCoderParametersQueryPresenter.QueryParams] => QueryContext[
        AtCoderParametersQueryPresenter.QueryParams
      ]
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
    boostrapParameters: AtCoderBootstrapParameters
  ): QueryContext[AtCoderParametersQueryPresenter.QueryParams] =
    QueryContext[AtCoderParametersQueryPresenter.QueryParams](
      AtCoderParametersQueryPresenter.QueryParams(None, None),
      Pagination()
    )

  override protected def executeQuery(
    context: QueryContext[AtCoderParametersQueryPresenter.QueryParams]
  ): IO[(Pagination, List[AtCoderTableItem])] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource[IO]
      .use { dsl =>
        val state = context.criteria
        val from  = math.max((context.pagination.currentPage - 1) * context.pagination.pageSize.value, 0)
        val limit = context.pagination.pageSize.value
        IO.delay {

          val difficultyCondition = state.selectedDifficulty.map { difficulty =>
            val (minRating, maxRating) = AtCoderDifficulty.atCoderDifficultyRange(difficulty)
            ATCODER_PROBLEMS.DIFFICULTY.between(minRating, maxRating)
          }.getOrElse(DSL.noCondition())
          val total = dsl
            .selectCount()
            .from(ATCODER_PROBLEMS)
            .where(difficultyCondition)
            .fetchOne(0, classOf[Int])

          val orderBy = state.orderBy.map {
            case (AtCoderSearchOrderBy.ContestId, dir) => dir.toJooqSortField(ATCODER_PROBLEMS.CONTESTID)
            case (AtCoderSearchOrderBy.Difficulty, dir) =>
              dir.toJooqSortField(ATCODER_PROBLEMS.DIFFICULTY)
          }

          val base = dsl
            .select(ATCODER_PROBLEMS.fields()*)
            .from(ATCODER_PROBLEMS)
            .where(difficultyCondition)
          val query =
            orderBy match
              case None =>
                base
                  .limit(from, limit)
                  .fetch()
                  .asScala
                  .map { record =>
                    AtCoderTableItem(record.into(classOf[AtcoderProblemsRecord]))
                  }
                  .toList
              case Some(orderBy) =>
                base
                  .orderBy(orderBy)
                  .limit(from, limit)
                  .fetch()
                  .asScala
                  .map { record => AtCoderTableItem(record.into(classOf[AtcoderProblemsRecord])) }
                  .toList

          (context.pagination.copy(totalSize = total), query)
        }
      }

  def getDirectionOf(field: AtCoderSearchOrderBy): Option[OrderDirection] =
    myQueryStateManager.get.criteria.orderBy.collect {
      case (f, d) if f == field => d
    }

  def setDirectionOf(field: AtCoderSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    direction match
      case None            => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(None))
      case Some(direction) => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(Some((field, direction))))
    requery(true)
  }

  override def getQueryResultColumns: Array[OrderByColumnInfo[AtCoderTableItem, ?]] = Array(
    new OrderByColumnInfo[AtCoderTableItem, String]("ProblemId") {
      override def valueOf(item: AtCoderTableItem): String =
        item.record.getProblemid

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)
      override def enableOrderBy: Boolean          = false
    },
    new OrderByColumnInfo[AtCoderTableItem, String]("Title") {
      override def valueOf(item: AtCoderTableItem): String = item.record.getTitle

      override def getPreferredStringValue: String = StringUtil.repeat("W", 20)

      override def enableOrderBy: Boolean = false
    },
    new OrderByColumnInfo[AtCoderTableItem, Option[AtCoderDifficulty]]("Difficulty") {
      override def valueOf(item: AtCoderTableItem): Option[AtCoderDifficulty] =
        Option(item.record.getDifficulty)
          .map(it => AtCoderDifficulty.fromInt(it.asInstanceOf[Int]))

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(AtCoderSearchOrderBy.Difficulty)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(AtCoderSearchOrderBy.Difficulty, filter)

      override def getRenderer(item: AtCoderTableItem): TableCellRenderer = {
        new IconTableCellRenderer[Option[AtCoderDifficulty]]() {
          override def getIcon(value: Option[AtCoderDifficulty], table: JTable, row: Int): Icon =
            value match {
              case None =>
                setText("")
                null
              case Some(difficulty) =>
                setText(difficulty.showAsHtml(item.record.getDifficulty))
                ColorIcon(12, difficulty.color, true)
            }
        }
      }
    },
    new OrderByColumnInfo[AtCoderTableItem, String]("Contest Id") {
      override def valueOf(item: AtCoderTableItem): String = item.record.getContestid

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)

      override def enableOrderBy: Boolean = true
      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(AtCoderSearchOrderBy.ContestId)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(AtCoderSearchOrderBy.ContestId, filter)
    },
    new OrderByColumnInfo[AtCoderTableItem, String]("Contest Title") {
      override def valueOf(item: AtCoderTableItem): String = item.record.getContesttitle

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)

      override def enableOrderBy: Boolean = false
    }
  )

}

object AtCoderParametersQueryPresenter {
  case class QueryParams(
    selectedDifficulty: Option[AtCoderDifficulty],
    orderBy: Option[(AtCoderSearchOrderBy, OrderDirection)]
  )

  private val DIFFICULTY_TAG_RADIUS = 0.3f
}
