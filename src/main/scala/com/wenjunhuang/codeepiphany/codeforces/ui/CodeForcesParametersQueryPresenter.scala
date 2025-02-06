package com.wenjunhuang.codeepiphany.codeforces.ui

import cats.effect.IO
import monocle.syntax.all.*
import org.jooq.impl.DSL
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.actions.{DifficultyParameterAction, OpenChallengeActionGroup, TagsAction}
import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.DifficultyParameterProvider
import com.wenjunhuang.codeepiphany.actions.TagsAction.{SingleTagGroupProvider, Tag}
import com.wenjunhuang.codeepiphany.codeforces.models.{codeForcesDifficultyToRatingRange, CodeForcesSearchOrderBy}
import com.wenjunhuang.codeepiphany.codeforces.ui.CodeForcesParametersQueryPresenter.*
import com.wenjunhuang.codeepiphany.database.tables.records.CodeforcesProblemsetsRecord
import com.wenjunhuang.codeepiphany.database.Tables.CODEFORCES_PROBLEMSETS_FTS
import com.wenjunhuang.codeepiphany.model.{Actions, ChallengeDifficulty, ChallengeRepository, OrderDirection}
import com.wenjunhuang.codeepiphany.services.{ParametersQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

class CodeForcesParametersQueryPresenter(project: Project, bootstrap: CodeForcesBootstrapParameters)
    extends ParametersQueryPresenter[
      CodeForcesBootstrapParameters,
      CodeForcesParametersQueryPresenter.QueryParams,
      CodeforcesProblemsetsRecord
    ](project, bootstrap) {

  override protected def prepareProviders(
    getter: () => QueryContext[CodeForcesParametersQueryPresenter.QueryParams],
    updater: (
      QueryContext[CodeForcesParametersQueryPresenter.QueryParams] => QueryContext[
        CodeForcesParametersQueryPresenter.QueryParams
      ]
    ) => Unit,
    dataSink: DataSink
  ): ActionGroup = {
    val tagProvider = new SingleTagGroupProvider {
      override def getAllItems: List[Tag] = myBoostrapParameters.tags.map(t => Tag(t, t, t))

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: Tag): Boolean = getter().criteria.selectedTags.contains(item)

      override def getSelectedItems: List[Tag] = getter().criteria.selectedTags

      override def addSelectedItems(items: List[Tag]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedTags).modify(it => (it ++ items).distinct)
        }
      }

      override def toggleSelection(item: Tag): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedTags).modify { tags =>
            if tags.contains(item) then tags.filterNot(_ == item)
            else (tags :+ item).distinct
          }
        }
      }

      override def removeSelectedItems(items: List[Tag]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedTags).modify(tags => tags.filterNot(items.contains))
        }
      }
    }
    val difficultyParameterProvider = new DifficultyParameterProvider {
      override def isSelected(item: ChallengeDifficulty): Boolean =
        getter().criteria.selectedDifficulty.contains(item)

      override def isMultipleSelection: Boolean = false

      override def toggleSelection(item: ChallengeDifficulty): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedDifficulty).modify {
            case Some(selected) if selected == item => None
            case _                                  => Some(item)
          }
        }
      }

      override def getAllItems: List[ChallengeDifficulty] =
        List(ChallengeDifficulty.Easy, ChallengeDifficulty.Medium, ChallengeDifficulty.Hard)

      override def getSelectedItems: List[ChallengeDifficulty] = getter().criteria.selectedDifficulty.toList

      override def addSelectedItems(items: List[ChallengeDifficulty]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedDifficulty).replace(items.headOption)
        }
      }

      override def removeSelectedItems(items: List[ChallengeDifficulty]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedDifficulty).replace(None)
        }
      }
    }

    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createCodeForcesChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel)
    )
    dataSink.set(TagsAction.TAG_PROVIDER_KEY, tagProvider)
    dataSink.set(DifficultyParameterAction.DIFFICULTIES_PROVIDER_KEY, difficultyParameterProvider)
    ActionManager.getInstance().getAction(Actions.CODEFORCES_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  }

  override protected def createQueryParametersTags(
    context: QueryContext[CodeForcesParametersQueryPresenter.QueryParams],
    onCloseUpdater: (
      QueryContext[CodeForcesParametersQueryPresenter.QueryParams] => QueryContext[
        CodeForcesParametersQueryPresenter.QueryParams
      ]
    ) => Unit
  ): List[TagPaneAction] = {
    context.criteria.selectedDifficulty.map { difficulty =>
      TagPaneAction(
        difficulty.value,
        difficulty.showAsHtml,
        None,
        DIFFICULTY_TAG_RADIUS,
        None,
        Some(() => onCloseUpdater(_.focus(_.criteria.selectedDifficulty).modify(_ => None)))
      )
    }.toList ++
      context.criteria.selectedTags.map { tag =>
        TagPaneAction(
          tag.value,
          tag.name,
          None,
          TAG_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(_.focus(_.criteria.selectedTags).modify(tags => tags.filterNot(_ == tag))))
        )
      }
  }

  override protected def createInitialQueryParameters(
    boostrapParameters: CodeForcesBootstrapParameters
  ): QueryContext[CodeForcesParametersQueryPresenter.QueryParams] =
    QueryContext[CodeForcesParametersQueryPresenter.QueryParams](
      criteria = QueryParams(None, Nil, None),
      pagination = Pagination()
    )

  override protected def executeQuery(
    context: QueryContext[CodeForcesParametersQueryPresenter.QueryParams]
  ): IO[(Pagination, List[CodeforcesProblemsetsRecord])] = ChallengeRepository
    .getInstance(myProject)
    .getDSLContextResource[IO]
    .use { dsl =>
      val state = context.criteria
      val from  = math.max((context.pagination.currentPage - 1) * context.pagination.pageSize.value, 0)
      val limit = context.pagination.pageSize.value
      IO.delay {
        val tagCondition =
          if state.selectedTags.isEmpty then DSL.trueCondition()
          else
            val matches = s"${CODEFORCES_PROBLEMSETS_FTS.TAGS.getUnqualifiedName}:${state.selectedTags
                .map(tag => tag.value)
                .mkString(" + ")}"
            DSL.condition("{0} MATCH {1}", DSL.field(CODEFORCES_PROBLEMSETS_FTS.getUnqualifiedName), matches)
        val diffCondition =
          if state.selectedDifficulty.isEmpty then DSL.trueCondition()
          else
            val (minRating, maxRating) = codeForcesDifficultyToRatingRange(state.selectedDifficulty.get)
            CODEFORCES_PROBLEMSETS_FTS.RATING.between(minRating, maxRating)
        val condition = tagCondition.and(diffCondition)
        val total = dsl
          .selectCount()
          .from(CODEFORCES_PROBLEMSETS_FTS)
          .where(condition)
          .fetchOne(0, classOf[Int])

        val orderBy = state.orderBy.map {
          case (CodeForcesSearchOrderBy.Rating, dir) => dir.toJooqSortField(CODEFORCES_PROBLEMSETS_FTS.RATING)
          case (CodeForcesSearchOrderBy.ContestIdIndex, dir) =>
            dir.toJooqSortField(CODEFORCES_PROBLEMSETS_FTS.CONTESTIDINDEX)
        }

        val query = dsl
          .selectFrom(CODEFORCES_PROBLEMSETS_FTS)
          .where(condition)
          .orderBy(orderBy.toList*)
          .limit(from, limit)
          .fetchInto(classOf[CodeforcesProblemsetsRecord])
          .asScala
          .toList

        (context.pagination.copy(totalSize = total), query)
      }
    }

  def getDirectionOf(field: CodeForcesSearchOrderBy): Option[OrderDirection] =
    myQueryStateManager.get.criteria.orderBy.collect {
      case (f, d) if f == field => d
    }

  def setDirectionOf(field: CodeForcesSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    direction match
      case None            => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(None))
      case Some(direction) => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(Some((field, direction))))
    requery()
  }

  override def getQueryResultColumns: Array[OrderByColumnInfo[CodeforcesProblemsetsRecord, ?]] = Array(
    new OrderByColumnInfo[CodeforcesProblemsetsRecord, String]("#") {
      override def valueOf(item: CodeforcesProblemsetsRecord): String =
        s"""${Option(item.getContestid).map(_.toString).getOrElse("")}${Option(item.getIndex).getOrElse("")}"""

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] = getDirectionOf(CodeForcesSearchOrderBy.ContestIdIndex)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(CodeForcesSearchOrderBy.ContestIdIndex, filter)
    },
    new OrderByColumnInfo[CodeforcesProblemsetsRecord, String]("Title") {
      override def valueOf(item: CodeforcesProblemsetsRecord): String = item.getName

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)

      override def enableOrderBy: Boolean = false
    },
    new OrderByColumnInfo[CodeforcesProblemsetsRecord, String]("Difficulty") {
      override def valueOf(item: CodeforcesProblemsetsRecord): String =
        Option(item.getRating).map(_.toString).getOrElse("")

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(CodeForcesSearchOrderBy.Rating)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(CodeForcesSearchOrderBy.Rating, filter)
    }
  )

}

object CodeForcesParametersQueryPresenter {
  case class QueryParams(
    selectedDifficulty: Option[ChallengeDifficulty],
    selectedTags: List[Tag],
    orderBy: Option[(CodeForcesSearchOrderBy, OrderDirection)]
  )

  private val DIFFICULTY_TAG_RADIUS = 0.3f
  private val TAG_TAG_RADIUS        = 1.0f
}
