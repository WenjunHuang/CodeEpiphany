package com.wenjunhuang.codeepiphany.leetcode.ui

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.table.IconTableCellRenderer
import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.{DIFFICULTIES_PROVIDER_KEY, DifficultyParameterProvider}
import com.wenjunhuang.codeepiphany.actions.StatusParameterAction.{STATUS_PROVIDER_KEY, StatusParameterProvider}
import com.wenjunhuang.codeepiphany.actions.TagsAction.*
import com.wenjunhuang.codeepiphany.actions.{OpenChallengeActionGroup, TagsAction}
import com.wenjunhuang.codeepiphany.leetcode.actions.FavoriteParameterAction.{FAVORITE_PROVIDER_KEY, FavoriteParameterProvider}
import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeCategoryParameterAction.{LEETCODE_CATEGORY_PROVIDER_KEY, LeetCodeCategoryProvider}
import com.wenjunhuang.codeepiphany.leetcode.models.*
import com.wenjunhuang.codeepiphany.leetcode.services.{LeetCodeApi, LeetCodeSearchOrderBy}
import com.wenjunhuang.codeepiphany.leetcode.settings.{LeetCodeCNSettings, LeetCodeSettings}
import com.wenjunhuang.codeepiphany.leetcode.ui.LeetCodeParametersQueryPresenter.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager}
import com.wenjunhuang.codeepiphany.services.{ParametersQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.syntax.*
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, PageSize, Pagination}
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import monocle.syntax.all.*

import javax.swing.table.{DefaultTableCellRenderer, TableCellRenderer}
import javax.swing.{Icon, JTable, SwingConstants}

class LeetCodeParametersQueryPresenter(
  project: Project,
  boostrap: LeetCodeBootstrapParameters,
  private val myLeetCodeDojo: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
) extends ParametersQueryPresenter[LeetCodeBootstrapParameters, LeetCodeQueryCriteria, LeetCodeChallengeListItem](
      project,
      boostrap
    ) {

  override protected def prepareProviders(
    getter: () => QueryContext[LeetCodeQueryCriteria],
    updater: (QueryContext[LeetCodeQueryCriteria] => QueryContext[LeetCodeQueryCriteria]) => Unit,
    dataSink: DataSink
  ): ActionGroup = {
    val myCategoryProvider = new LeetCodeCategoryProvider {
      override def getAllItems: List[LeetCodeCategoryListItem] = myBoostrapParameters.categories

      override def isMultipleSelection: Boolean = false

      override def isSelected(item: LeetCodeCategoryListItem): Boolean =
        getter().criteria.selectedCategory.contains(item)

      override def getSelectedItems: List[LeetCodeCategoryListItem] = getter().criteria.selectedCategory.toList

      override def addSelectedItems(items: List[LeetCodeCategoryListItem]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedCategory).replace(items.headOption)
        }
      }

      override def toggleSelection(item: LeetCodeCategoryListItem): Unit = {
        updater { old =>
          val v =
            if old.criteria.selectedCategory.contains(item) then None
            else Some(item)
          old.focus(_.criteria.selectedCategory).replace(v)
        }
      }

      override def removeSelectedItems(items: List[LeetCodeCategoryListItem]): Unit = {
        updater { old =>
          if old.criteria.selectedCategory.contains(items.head) then
            old.focus(_.criteria.selectedCategory).replace(None)
          else old
        }
      }
    }

    val myFavoriteProvider = new FavoriteParameterProvider {
      override def getAllItems: List[LeetCodeFavoriteItem] = myBoostrapParameters.favorites

      override def isMultipleSelection: Boolean = false

      override def isSelected(item: LeetCodeFavoriteItem): Boolean =
        getter().criteria.selectedFavorite.contains(item)

      override def getSelectedItems: List[LeetCodeFavoriteItem] = getter().criteria.selectedFavorite.toList

      override def addSelectedItems(items: List[LeetCodeFavoriteItem]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedFavorite).replace(items.headOption)
        }
      }

      override def toggleSelection(item: LeetCodeFavoriteItem): Unit = {
        updater { old =>
          val v = if old.criteria.selectedCategory.contains(item) then None else Some(item)
          old.focus(_.criteria.selectedFavorite).replace(v)
        }
      }

      override def removeSelectedItems(items: List[LeetCodeFavoriteItem]): Unit = {
        updater { old =>
          if old.criteria.selectedFavorite.contains(items.head) then
            old.focus(_.criteria.selectedFavorite).replace(None)
          else old
        }
      }
    }

    val myDifficultiesProvider = new DifficultyParameterProvider {
      override def isSelected(item: ChallengeDifficulty): Boolean = getter().criteria.selectedDifficulty.contains(item)

      override def isMultipleSelection: Boolean = false

      override def toggleSelection(item: ChallengeDifficulty): Unit = {
        updater { old =>
          if old.criteria.selectedDifficulty.contains(item) then old.focus(_.criteria.selectedDifficulty).replace(None)
          else old.focus(_.criteria.selectedDifficulty).replace(Some(item))
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
          if old.criteria.selectedDifficulty.contains(items.head) then
            old.focus(_.criteria.selectedDifficulty).replace(None)
          else old
        }
      }
    }

    val myStatusProvider = new StatusParameterProvider {
      private val allItems = List(ChallengeStatus.Solved, ChallengeStatus.Unsolved, ChallengeStatus.Tried)

      override def isSelected(item: ChallengeStatus): Boolean = getter().criteria.selectedStatus.contains(item)

      override def getAllItems: List[ChallengeStatus] = allItems

      override def isMultipleSelection: Boolean = false

      override def getSelectedItems: List[ChallengeStatus] =
        getter().criteria.selectedStatus.toList

      override def addSelectedItems(items: List[ChallengeStatus]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedStatus).replace(items.headOption)
        }
      }

      override def toggleSelection(item: ChallengeStatus): Unit = {
        updater { old =>
          if old.criteria.selectedStatus.contains(item) then old.focus(_.criteria.selectedStatus).replace(None)
          else old.focus(_.criteria.selectedStatus).replace(Some(item))
        }
      }

      override def removeSelectedItems(items: List[ChallengeStatus]): Unit = {
        updater { old =>
          if old.criteria.selectedStatus.contains(items.head) then old.focus(_.criteria.selectedStatus).replace(None)
          else old
        }
      }
    }

    val myTagProvider = new MultiTagGroupProvider {
      private val allTags = myBoostrapParameters.tagTypeWithTags.flatMap { item =>
        item.tagRelation.map { relation =>
          Tag(
            relation.tag.nameTranslated.filter(_.nonEmpty).getOrElse(relation.tag.name),
            relation.tag.slug,
            item.name,
            relation.tag
          )
        }
      }

      override def isSearchEnabled: Boolean = true

      override def searchTags(query: String): List[Tag] = Nil

      override def getTabs: List[TagsAction.TagGroupTab] = {
        val groups = myBoostrapParameters.tagTypeWithTags.map { item =>
          TagGroup(
            item.transName.getOrElse(item.name),
            item.name,
            item.tagRelation.map { relation =>
              Tag(
                relation.tag.nameTranslated
                  .filter(_.nonEmpty)
                  .getOrElse(relation.tag.name),
                relation.tag.slug,
                item.name,
                relation.tag
              )
            },
            item
          )
        }
        List(TagGroupTab("Tags", "tags", groups))
      }

      override def getAllItems: List[Tag] = allTags

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: Tag): Boolean = {
        getter().criteria.selectedTags.contains(item)
      }

      override def getSelectedItems: List[Tag] = getter().criteria.selectedTags

      override def addSelectedItems(items: List[Tag]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedTags).replace((old.criteria.selectedTags ++ items).distinct)
        }
      }

      override def toggleSelection(item: Tag): Unit = {
        updater { old =>
          if old.criteria.selectedTags.contains(item) then
            old.focus(_.criteria.selectedTags).replace(old.criteria.selectedTags.filterNot(_ == item))
          else old.focus(_.criteria.selectedTags).replace((old.criteria.selectedTags ++ List(item)).distinct)
        }
      }

      override def removeSelectedItems(items: List[Tag]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedTags).replace(old.criteria.selectedTags.filterNot(items.contains))
        }
      }
    }

    dataSink.set(LEETCODE_CATEGORY_PROVIDER_KEY, myCategoryProvider)
    dataSink.set(FAVORITE_PROVIDER_KEY, myFavoriteProvider)
    dataSink.set(DIFFICULTIES_PROVIDER_KEY, myDifficultiesProvider)
    dataSink.set(STATUS_PROVIDER_KEY, myStatusProvider)
    dataSink.set(TAG_PROVIDER_KEY, myTagProvider)

    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createLeetCodeChallengeProvider(
        myProject,
        myQueryResultSelectionModel,
        myQueryResultTableModel,
        myBoostrapParameters,
        myLeetCodeDojo
      )
    )

    ActionManager.getInstance().getAction(Actions.LEETCODE_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  }

  override protected def createQueryParametersTags(
    context: QueryContext[LeetCodeQueryCriteria],
    onCloseUpdater: (QueryContext[LeetCodeQueryCriteria] => QueryContext[LeetCodeQueryCriteria]) => Unit
  ): List[TagPaneAction] = {
    val myState = context.criteria

    myState.selectedCategory.map { category =>
      TagPaneAction(
        category.slug,
        category.title,
        None,
        CATEGORY_TAG_RADIUS,
        None,
        Some(() => onCloseUpdater(_.focus(_.criteria.selectedCategory).replace(None)))
      )
    }.toList ++
      myState.selectedFavorite.map { favorite =>
        TagPaneAction(
          favorite.id,
          favorite.name,
          None,
          FAVORITE_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(_.focus(_.criteria.selectedFavorite).replace(None)))
        )
      } ++
      myState.selectedDifficulty.map { difficulty =>
        TagPaneAction(
          difficulty.value,
          difficulty.showAsHtml,
          None,
          DIFFICULTY_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(_.focus(_.criteria.selectedDifficulty).replace(None)))
        )
      } ++
      myState.selectedStatus.map { status =>
        TagPaneAction(
          status.value,
          status.show,
          None,
          STATUS_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(_.focus(_.criteria.selectedStatus).replace(None)))
        )
      } ++
      myState.selectedTags.map { tag =>
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

  private def getDirectionOf(field: LeetCodeSearchOrderBy): Option[OrderDirection] =
    myQueryStateManager.get.criteria.orderBy.collect {
      case (f, d) if f == field => d
    }

  private def setDirectionOf(field: LeetCodeSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    myQueryStateManager.update { old =>
      direction match
        case None => old.focus(_.criteria.orderBy).replace(None)
        case Some(direction) =>
          old.focus(_.criteria.orderBy).replace(Some((field, direction)))
    }
    requery(true)
  }

  override protected def createInitialQueryParameters(
    boostrapParameters: LeetCodeBootstrapParameters
  ): QueryContext[LeetCodeQueryCriteria] =
    QueryContext(LeetCodeQueryCriteria(None, None, None, None, Nil, None), Pagination())

  override protected def executeQuery(
    context: QueryContext[LeetCodeQueryCriteria]
  ): IO[(Pagination, List[LeetCodeChallengeListItem])] = {
    val pageSize    = context.pagination.pageSize
    val currentPage = context.pagination.currentPage
    val orderBy     = context.criteria.orderBy
    LeetCodeApi(myLeetCodeDojo)
      .searchChallenges(
        pageSize.value * (currentPage - 1),
        pageSize.value,
        context.criteria.selectedCategory,
        context.criteria.selectedFavorite,
        context.criteria.selectedDifficulty,
        context.criteria.selectedStatus,
        context.criteria.selectedTags.map(_.userObj.asInstanceOf[LeetCodeTag]),
        orderBy
      )
      .map { response =>
        (context.pagination.copy(totalSize = response.total), response.questions)
      }
  }

  override protected def saveQueryCriteria(queryCriteria: LeetCodeQueryCriteria, pagination: Pagination): Unit =
    val queryCriteriaStore = myLeetCodeDojo match
      case CodeDojo.LeetCodeCN =>
        LeetCodeCNSettings
          .getInstance(myProject)
          .getState
          .queryCriteria
      case CodeDojo.LeetCode =>
        LeetCodeSettings
          .getInstance(myProject)
          .getState
          .queryCriteria
    queryCriteriaStore.put(s"${getClass.getSimpleName}-criteria", queryCriteria.asJson.noSpaces)
    queryCriteriaStore.put(s"${getClass.getSimpleName}-pageSize", pagination.pageSize.value.toString)

  override protected def loadQueryCriteria(): Option[(LeetCodeQueryCriteria, Pagination)] =
    val queryCriteriaStore = myLeetCodeDojo match
      case CodeDojo.LeetCodeCN =>
        LeetCodeCNSettings
          .getInstance(myProject)
          .getState
          .queryCriteria
      case CodeDojo.LeetCode =>
        LeetCodeSettings
          .getInstance(myProject)
          .getState
          .queryCriteria
    Option(queryCriteriaStore.get(s"${getClass.getSimpleName}-criteria"))
      .flatMap(value => decode[LeetCodeQueryCriteria](value).toOption)
      .zip(
        Option(queryCriteriaStore.get(s"${getClass.getSimpleName}-pageSize"))
          .flatMap(value => decode[PageSize](value).toOption)
          .map(v => Pagination(pageSize = v))
      )
      .map(identity)

  override def getQueryResultColumns: Array[OrderByColumnInfo[LeetCodeChallengeListItem, ?]] = {
    import LeetCodeTableColumnTitle.*
    val userIsPremium = myBoostrapParameters.userInfo.isPremium.getOrElse(false)

    def isRowEnabled(item: LeetCodeChallengeListItem) = {
      !item.paidOnly || (item.paidOnly && userIsPremium)
    }

    Array(
      new OrderByColumnInfo[LeetCodeChallengeListItem, Icon](Status.title) {
        override def valueOf(item: LeetCodeChallengeListItem): Icon =
          if item.paidOnly && !userIsPremium then AllIcons.Diff.Lock
          else
            item.status
              .map(myLeetCodeDojo.fromLeetCodeStatus)
              .getOrElse(ChallengeStatus.Unsolved) match
              case ChallengeStatus.Solved   => AllIcons.General.InspectionsOK
              case ChallengeStatus.Tried    => AllIcons.General.Modified
              case ChallengeStatus.Unsolved => null

        override def getPreferredStringValue: String = Status.title

        override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
          new IconTableCellRenderer[Icon]() {
            override def getIcon(value: Icon, table: JTable, row: Int): Icon = value

            override def isCenterAlignment: Boolean = true

            override def getText: String = null
          }
      },
      new OrderByColumnInfo[LeetCodeChallengeListItem, String](Title.title) {
        override def valueOf(item: LeetCodeChallengeListItem): String =
          s"[${item.frontendQuestionId}]${item.titleCn.filter(_.nonEmpty).getOrElse(item.title)}"

        override def getPreferredStringValue: String = StringUtil.repeat("W", 30)

        override def enableOrderBy: Boolean = true

        override def getOrderFilter: Option[OrderDirection] = getDirectionOf(LeetCodeSearchOrderBy.FontEndId)

        override def setOrderFilter(filter: Option[OrderDirection]): Unit = {
          setDirectionOf(LeetCodeSearchOrderBy.FontEndId, filter)
        }

        override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalTextPosition(SwingConstants.LEADING)
            if item.paidOnly && userIsPremium then setIcon(AllIcons.Ide.Readwrite)
            setEnabled(isRowEnabled(item))
          }
      },
      new OrderByColumnInfo[LeetCodeChallengeListItem, String](Solution.title) {
        override def valueOf(item: LeetCodeChallengeListItem): String = item.solutionNum.toString

        override def getPreferredStringValue: String = Solution.title

        override def enableOrderBy: Boolean = true

        override def getOrderFilter: Option[OrderDirection] =
          getDirectionOf(LeetCodeSearchOrderBy.SolutionNum)

        override def setOrderFilter(filter: Option[OrderDirection]): Unit = {
          setDirectionOf(LeetCodeSearchOrderBy.SolutionNum, filter)
        }

        override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
            setEnabled(isRowEnabled(item))
          }
      },
      new OrderByColumnInfo[LeetCodeChallengeListItem, String](Difficulty.title) {
        override def valueOf(item: LeetCodeChallengeListItem): String =
          myLeetCodeDojo.fromLeetCodeDifficulty(item.difficulty).showAsHtml

        override def enableOrderBy: Boolean = true

        override def getOrderFilter: Option[OrderDirection] =
          getDirectionOf(LeetCodeSearchOrderBy.Difficulty)

        override def setOrderFilter(filter: Option[OrderDirection]): Unit =
          setDirectionOf(LeetCodeSearchOrderBy.Difficulty, filter)

        override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setEnabled(isRowEnabled(item))
          }
      },
      new OrderByColumnInfo[LeetCodeChallengeListItem, String](Acceptance.title) {
        override def valueOf(item: LeetCodeChallengeListItem): String =
          myLeetCodeDojo match
            case CodeDojo.LeetCode   => f"${item.acRate}%.2f%%"
            case CodeDojo.LeetCodeCN => f"${item.acRate * 100}%.2f%%"

        override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
            setEnabled(isRowEnabled(item))
          }

        override def enableOrderBy: Boolean = true

        override def getOrderFilter: Option[OrderDirection] =
          getDirectionOf(LeetCodeSearchOrderBy.ACRate)

        override def setOrderFilter(filter: Option[OrderDirection]): Unit =
          setDirectionOf(LeetCodeSearchOrderBy.ACRate, filter)
      },
      new OrderByColumnInfo[LeetCodeChallengeListItem, String](Frequency.title) {
        override def valueOf(item: LeetCodeChallengeListItem): String =
          item.freqBar match
            case None    => ""
            case Some(v) => f"${v}%.2f%%"

        override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
            setEnabled(isRowEnabled(item))
          }

        override def enableOrderBy: Boolean = true

        override def getOrderFilter: Option[OrderDirection] =
          getDirectionOf(LeetCodeSearchOrderBy.Frequency)

        override def setOrderFilter(filter: Option[OrderDirection]): Unit =
          setDirectionOf(LeetCodeSearchOrderBy.Frequency, filter)
      }
    )
  }
}

object LeetCodeParametersQueryPresenter {
  private val CATEGORY_TAG_RADIUS   = 0.1f
  private val FAVORITE_TAG_RADIUS   = 0.2f
  private val DIFFICULTY_TAG_RADIUS = 0.3f
  private val STATUS_TAG_RADIUS     = 0.4f
  private val TAG_TAG_RADIUS        = 1.0f

  case class LeetCodeQueryCriteria(
    selectedCategory: Option[LeetCodeCategoryListItem],
    selectedFavorite: Option[LeetCodeFavoriteItem],
    selectedDifficulty: Option[ChallengeDifficulty],
    selectedStatus: Option[ChallengeStatus],
    selectedTags: List[Tag],
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
  )

  object LeetCodeQueryCriteria {
    implicit val circeEncoder: Encoder[LeetCodeQueryCriteria] = Encoder.instance { criteria =>
      Json.obj(
        "selectedCategory"   := criteria.selectedCategory,
        "selectedFavorite"   := criteria.selectedFavorite,
        "selectedDifficulty" := criteria.selectedDifficulty,
        "selectedStatus"     := criteria.selectedStatus,
        "tags"               -> leetCodeTagToJson(criteria.selectedTags),
        "orderBy"            := criteria.orderBy
      )
    }

    implicit val circeDecoder: Decoder[LeetCodeQueryCriteria] = Decoder.instance { cursor =>
      for {
        selectedCategory   <- cursor.downField("selectedCategory").as[Option[LeetCodeCategoryListItem]]
        selectedFavorite   <- cursor.downField("selectedFavorite").as[Option[LeetCodeFavoriteItem]]
        selectedDifficulty <- cursor.downField("selectedDifficulty").as[Option[ChallengeDifficulty]]
        selectedStatus     <- cursor.downField("selectedStatus").as[Option[ChallengeStatus]]
        tags               <- cursor.downField("tags").as[Json].flatMap(leetCodeTagFromJson)
        orderBy            <- cursor.downField("orderBy").as[Option[(LeetCodeSearchOrderBy, OrderDirection)]]
      } yield LeetCodeQueryCriteria(
        selectedCategory,
        selectedFavorite,
        selectedDifficulty,
        selectedStatus,
        tags,
        orderBy
      )
    }
  }
}
