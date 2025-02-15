package com.wenjunhuang.codeepiphany.luogu.ui

import cats.syntax.all.*
import cats.effect.IO
import javax.swing.SwingConstants
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }
import monocle.syntax.all.*

import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.actions.{ OpenChallengeActionGroup, TagsAction }
import com.wenjunhuang.codeepiphany.actions.TagsAction.MultiTagGroupProvider
import com.wenjunhuang.codeepiphany.luogu.actions.{ LuoGuDifficultyParameterAction, LuoGuQuestionBankParameterAction }
import com.wenjunhuang.codeepiphany.luogu.actions.LuoGuDifficultyParameterAction.LuoGuDifficultyParameterProvider
import com.wenjunhuang.codeepiphany.luogu.actions.LuoGuQuestionBankParameterAction.LuoGuQuestionBankParameterProvider
import com.wenjunhuang.codeepiphany.luogu.models.*
import com.wenjunhuang.codeepiphany.luogu.services.LuoGuApi
import com.wenjunhuang.codeepiphany.luogu.ui.LuoGuParametersQueryPresenter.{
  DIFFICULTY_TAG_RADIUS,
  QUESTION_BANK_RADIUS,
  TAG_TAG_RADIUS
}
import com.wenjunhuang.codeepiphany.model.{ Actions, OrderDirection }
import com.wenjunhuang.codeepiphany.services.{ ParametersQueryPresenter, QueryContext }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.{ OrderByColumnInfo, PageSize, Pagination }
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction
import com.wenjunhuang.codeepiphany.utils.PageSize.Fifty

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
    val questionBankProvider = new LuoGuQuestionBankParameterProvider {
      override def getAllItems: List[LuoGuQuestionBank] = LuoGuQuestionBank.values.toList

      override def isMultipleSelection: Boolean = false

      override def isSelected(item: LuoGuQuestionBank): Boolean = getter().criteria.selectedQuestionBank.contains(item)

      override def getSelectedItems: List[LuoGuQuestionBank] = getter().criteria.selectedQuestionBank.toList

      override def addSelectedItems(items: List[LuoGuQuestionBank]): Unit = updater { old =>
        old.focus(_.criteria.selectedQuestionBank).replace(items.headOption)
      }

      override def toggleSelection(item: LuoGuQuestionBank): Unit = updater { old =>
        if old.criteria.selectedQuestionBank.contains(item) then
          old.focus(_.criteria.selectedQuestionBank).replace(None)
        else old.focus(_.criteria.selectedQuestionBank).replace(Some(item))
      }

      override def removeSelectedItems(items: List[LuoGuQuestionBank]): Unit = updater { old =>
        old.focus(_.criteria.selectedQuestionBank).replace(None)
      }
    }
    val difficultyProvider = new LuoGuDifficultyParameterProvider {
      override def getAllItems: List[LuoGuDifficulty] = LuoGuDifficulty.values.toList

      override def isMultipleSelection: Boolean = false

      override def isSelected(item: LuoGuDifficulty): Boolean = getter().criteria.selectedDifficulty.contains(item)

      override def getSelectedItems: List[LuoGuDifficulty] = getter().criteria.selectedDifficulty.toList

      override def addSelectedItems(items: List[LuoGuDifficulty]): Unit = updater { old =>
        old.focus(_.criteria.selectedDifficulty).replace(items.headOption)
      }

      override def toggleSelection(item: LuoGuDifficulty): Unit = updater { old =>
        if old.criteria.selectedDifficulty.contains(item) then old.focus(_.criteria.selectedDifficulty).replace(None)
        else old.focus(_.criteria.selectedDifficulty).replace(Some(item))
      }

      override def removeSelectedItems(items: List[LuoGuDifficulty]): Unit = updater { old =>
        old.focus(_.criteria.selectedDifficulty).replace(None)
      }
    }
    val tagProvider = new MultiTagGroupProvider {
      private lazy val myTabs = LuoGuTagTypeWithTags.ALL_TAG_TYPES.map { tagType =>
        val groups = tagType.tagGroups.map { tg =>
          val tags = tg.tags.map { tag =>
            TagsAction.Tag(tag.name, tag.id.toString, tg.id.toString, tag)
          }
          TagsAction.TagGroup(tg.name, tg.id.toString, tags, tg)
        }
        TagsAction.TagGroupTab(tagType.value, tagType.id, groups, tagType)
      }
      override def isSearchEnabled: Boolean = true

      override def searchTags(query: String): List[TagsAction.Tag] = Nil

      override def getTabs: List[TagsAction.TagGroupTab] = myTabs

      override def getAllItems: List[TagsAction.Tag] =
        myTabs.flatMap(_.tagGroups.flatMap(_.tags))

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: TagsAction.Tag): Boolean = getter().criteria.selectedTags.contains(item)

      override def getSelectedItems: List[TagsAction.Tag] = getter().criteria.selectedTags

      override def addSelectedItems(items: List[TagsAction.Tag]): Unit = updater { old =>
        old.focus(_.criteria.selectedTags).modify(it => (it ++ items).distinct)
      }

      override def toggleSelection(item: TagsAction.Tag): Unit = updater { old =>
        if old.criteria.selectedTags.contains(item) then
          old.focus(_.criteria.selectedTags).modify(_.filterNot(_ == item))
        else old.focus(_.criteria.selectedTags).modify(item +: _)
      }

      override def removeSelectedItems(items: List[TagsAction.Tag]): Unit = updater { old =>
        old.focus(_.criteria.selectedTags).modify(_.filterNot(items.contains))
      }
    }

    dataSink.set(LuoGuDifficultyParameterAction.LUOGU_DIFFICULTY_PROVIDER_KEY, difficultyProvider)
    dataSink.set(LuoGuQuestionBankParameterAction.LUOGU_QUESTION_BANK_PROVIDER_KEY, questionBankProvider)
    dataSink.set(TagsAction.TAG_PROVIDER_KEY, tagProvider)
    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createLuoGuChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel)
    )
    ActionManager.getInstance().getAction(Actions.LUOGU_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  }

  override protected def createQueryParametersTags(
    context: QueryContext[LuoGuParametersQueryPresenter.QueryParams],
    onCloseUpdater: (
      QueryContext[LuoGuParametersQueryPresenter.QueryParams] => QueryContext[LuoGuParametersQueryPresenter.QueryParams]
    ) => Unit
  ): List[TagPaneAction] = {
    context.criteria.selectedQuestionBank.map { bank =>
      TagPaneAction(
        bank.value,
        bank.show,
        None,
        QUESTION_BANK_RADIUS,
        None,
        Some(() => onCloseUpdater(_.focus(_.criteria.selectedQuestionBank).modify(_ => None)))
      )
    }.toList ++
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
      ++ context.criteria.selectedTags.map { tag =>
        TagPaneAction(
          tag.value,
          tag.name,
          None,
          TAG_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(_.focus(_.criteria.selectedTags).modify(_.filterNot(_ == tag))))
        )
      }
  }

  override protected def createInitialQueryParameters(
    boostrapParameters: LuoGuBootstrapParameters
  ): QueryContext[LuoGuParametersQueryPresenter.QueryParams] =
    QueryContext[LuoGuParametersQueryPresenter.QueryParams](
      LuoGuParametersQueryPresenter.QueryParams(None, None, Nil, None),
      Pagination(pageSize = PageSize.Fifty)
    )

  override protected def pageSizes: List[PageSize] = List(Fifty)

  override protected def executeQuery(
    context: QueryContext[LuoGuParametersQueryPresenter.QueryParams]
  ): IO[(Pagination, List[LuoGuChallengeItem])] = {
    implicit val httpClient: HttpClientManager[IO] = HttpClientService.getInstance(myProject).httpClientManager
    LuoGuApi[IO]()
      .searchChallenges(
        context.criteria.selectedDifficulty,
        context.criteria.selectedQuestionBank,
        context.criteria.selectedTags.map(_.userObj.asInstanceOf[LuoGuTag]),
        context.criteria.orderBy,
        context.pagination.currentPage
      )
      .map { case (total, items) =>
        (context.pagination.copy(totalSize = total), items)
      }
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

      override def getPreferredStringValue: String = StringUtil.repeat("W", 5)
      override def enableOrderBy: Boolean          = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(LuoGuSearchOrderBy.PID)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(LuoGuSearchOrderBy.PID, filter)
    },
    new OrderByColumnInfo[LuoGuChallengeItem, String]("Title") {
      override def valueOf(item: LuoGuChallengeItem): String = item.title

      override def getPreferredStringValue: String = StringUtil.repeat("W", 20)

      override def enableOrderBy: Boolean = true
      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(LuoGuSearchOrderBy.Title)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(LuoGuSearchOrderBy.Title, filter)
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
        if item.totalSubmit <= 0 then ""
        else f"${(item.totalAccepted.toDouble / item.totalSubmit.toDouble) * 100}%.2f%%"

      override def getRenderer(item: LuoGuChallengeItem): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }
    }
  )

}

object LuoGuParametersQueryPresenter {
  case class QueryParams(
    selectedQuestionBank: Option[LuoGuQuestionBank],
    selectedDifficulty: Option[LuoGuDifficulty],
    selectedTags: List[TagsAction.Tag],
    orderBy: Option[(LuoGuSearchOrderBy, OrderDirection)]
  )

  private val QUESTION_BANK_RADIUS  = 0.1f
  private val DIFFICULTY_TAG_RADIUS = 0.3f
  private val TAG_TAG_RADIUS        = 1.0f
}
