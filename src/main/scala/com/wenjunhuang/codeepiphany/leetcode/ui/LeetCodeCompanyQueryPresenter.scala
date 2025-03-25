package com.wenjunhuang.codeepiphany.leetcode.ui

import cats.effect.IO
import cats.syntax.all.*
import io.circe.*
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*
import javax.swing.{ Icon, JTable, SwingConstants }
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }
import monocle.syntax.all.*
import org.typelevel.log4cats.LoggerFactory

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.actions.{ OpenChallengeActionGroup, TagsAction }
import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.{
  DIFFICULTIES_PROVIDER_KEY,
  DifficultyParameterProvider
}
import com.wenjunhuang.codeepiphany.actions.StatusParameterAction.{ STATUS_PROVIDER_KEY, StatusParameterProvider }
import com.wenjunhuang.codeepiphany.actions.TagsAction.*
import com.wenjunhuang.codeepiphany.leetcode.actions.CompanyParameterAction.{
  COMPANY_PROVIDER_KEY,
  CompanyParameterProvider
}
import com.wenjunhuang.codeepiphany.leetcode.actions.InterviewPeriodParameterAction
import com.wenjunhuang.codeepiphany.leetcode.actions.InterviewPeriodParameterAction.{
  INTERVIEW_PERIOD_PROVIDER_KEY,
  InterviewPeriod,
  InterviewPeriodProvider
}
import com.wenjunhuang.codeepiphany.leetcode.actions.PositionParameterAction.{
  POSITION_PROVIDER_KEY,
  PositionParameterProvider
}
import com.wenjunhuang.codeepiphany.leetcode.models.*
import com.wenjunhuang.codeepiphany.leetcode.services.{ LeetCodeApi, LeetCodeSearchOrderBy }
import com.wenjunhuang.codeepiphany.leetcode.settings.{ LeetCodeCNSettings, LeetCodeSettings }
import com.wenjunhuang.codeepiphany.leetcode.ui.LeetCodeCompanyQueryPresenter.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.{ ParametersQueryPresenter, QueryContext }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.{ OrderByColumnInfo, PageSize, Pagination }
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

class LeetCodeCompanyQueryPresenter(
  project: Project,
  boostrap: LeetCodeBootstrapParameters,
  private val myLeetCodeDojo: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
) extends ParametersQueryPresenter[
      LeetCodeBootstrapParameters,
      LeetCodeCompanyQueryCriteria,
      LeetCodeChallengeListItem
    ](project, boostrap) {
  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  override protected def prepareProviders(
    getter: () => QueryContext[LeetCodeCompanyQueryCriteria],
    updater: (QueryContext[LeetCodeCompanyQueryCriteria] => QueryContext[LeetCodeCompanyQueryCriteria]) => Unit,
    dataSink: DataSink
  ): ActionGroup = {

    val companyProvider = new CompanyParameterProvider {
      override def getAllItems: List[LeetCodeQuestionCompanyTag] =
        myBoostrapParameters.companies

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: LeetCodeQuestionCompanyTag): Boolean =
        getter().criteria.selectedCompanies.contains(item)

      override def getSelectedItems: List[LeetCodeQuestionCompanyTag] =
        getter().criteria.selectedCompanies.toList

      override def addSelectedItems(items: List[LeetCodeQuestionCompanyTag]): Unit = updater { old =>
        old.focus(_.criteria.selectedCompanies).modify { companies =>
          companies ++ items
        }
      }

      override def toggleSelection(item: LeetCodeQuestionCompanyTag): Unit = updater { old =>
        old.focus(_.criteria.selectedCompanies).modify { companies =>
          if companies.contains(item) then companies.filter(_ != item)
          else companies + item
        }
      }

      override def removeSelectedItems(items: List[LeetCodeQuestionCompanyTag]): Unit = updater { old =>
        old.focus(_.criteria.selectedCompanies).modify { companies =>
          companies.filterNot(items.contains)
        }
      }
    }

    val positionProvider = new PositionParameterProvider {
      override def getAllItems: List[LeetCodeProblemsetPositionTag] = myBoostrapParameters.positions

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: LeetCodeProblemsetPositionTag): Boolean =
        getter().criteria.selectedPositions.contains(item)

      override def getSelectedItems: List[LeetCodeProblemsetPositionTag] = getter().criteria.selectedPositions

      override def addSelectedItems(items: List[LeetCodeProblemsetPositionTag]): Unit = updater { old =>
        old.focus(_.criteria.selectedPositions).modify { positions =>
          (positions ++ items).distinct
        }
      }

      override def toggleSelection(item: LeetCodeProblemsetPositionTag): Unit = updater { old =>
        old.focus(_.criteria.selectedPositions).modify { positions =>
          if positions.contains(item) then positions.filter(_ != item)
          else item +: positions
        }
      }

      override def removeSelectedItems(items: List[LeetCodeProblemsetPositionTag]): Unit = updater { old =>
        old.focus(_.criteria.selectedPositions).modify { positions =>
          positions.filterNot(items.contains)
        }
      }
    }
    val difficulties = new DifficultyParameterProvider {
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

    val statusProvider = new StatusParameterProvider {
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

    val interviewPeriodProvider = new InterviewPeriodProvider {
      override def getAllItems: List[InterviewPeriod] =
        InterviewPeriod.values.toList

      override def isMultipleSelection: Boolean = false

      override def isSelected(item: InterviewPeriodParameterAction.InterviewPeriod): Boolean =
        item == getter().criteria.selectedInterviewPeriod

      override def getSelectedItems: List[InterviewPeriodParameterAction.InterviewPeriod] = List(
        getter().criteria.selectedInterviewPeriod
      )

      override def addSelectedItems(items: List[InterviewPeriodParameterAction.InterviewPeriod]): Unit = updater {
        old =>
          if items.nonEmpty then old.focus(_.criteria.selectedInterviewPeriod).replace(items.head)
          else old
      }

      override def toggleSelection(item: InterviewPeriodParameterAction.InterviewPeriod): Unit = {}

      override def removeSelectedItems(items: List[InterviewPeriodParameterAction.InterviewPeriod]): Unit = {}
    }

    val tagProvider = new MultiTagGroupProvider {
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
    dataSink.set(COMPANY_PROVIDER_KEY, companyProvider)
    dataSink.set(POSITION_PROVIDER_KEY, positionProvider)
    dataSink.set(DIFFICULTIES_PROVIDER_KEY, difficulties)
    dataSink.set(INTERVIEW_PERIOD_PROVIDER_KEY, interviewPeriodProvider)
    dataSink.set(STATUS_PROVIDER_KEY, statusProvider)
    dataSink.set(TAG_PROVIDER_KEY, tagProvider)

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
    context: QueryContext[LeetCodeCompanyQueryCriteria],
    onCloseUpdater: (QueryContext[LeetCodeCompanyQueryCriteria] => QueryContext[LeetCodeCompanyQueryCriteria]) => Unit
  ): List[TagPaneAction] = {
    val myState = context.criteria

    myState.selectedCompanies.map { company =>
      TagPaneAction(
        company.slug,
        s"<html><span>${company.name}</span> <font color='#ffa116'>${company.questionCount}</font></html>",
        None,
        COMPANY_TAG_RADIUS,
        None,
        Some(() => onCloseUpdater(_.focus(_.criteria.selectedCompanies).modify(_.filterNot(_ == company))))
      )
    }.toList ++
      myState.selectedPositions.map { position =>
        TagPaneAction(
          position.slug,
          position.nameTranslated.filter(_.nonEmpty).getOrElse(position.name),
          None,
          POSITION_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(_.focus(_.criteria.selectedPositions).modify(_.filterNot(_ == position))))
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
      } ++
      List(
        TagPaneAction(
          myState.selectedInterviewPeriod.slug,
          myState.selectedInterviewPeriod.show,
          None,
          INTERVIEW_PERIOD_TAG_RADIUS,
          None,
          None
        )
      )
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
  ): QueryContext[LeetCodeCompanyQueryCriteria] =
    QueryContext(
      LeetCodeCompanyQueryCriteria(Set.empty, Nil, None, None, InterviewPeriod.ThirtyDays, Nil, None),
      Pagination()
    )

  override protected def executeQuery(
    context: QueryContext[LeetCodeCompanyQueryCriteria]
  ): IO[(Pagination, List[LeetCodeChallengeListItem])] = {
    if context.criteria.selectedCompanies.isEmpty then IO.pure((context.pagination.copy(totalSize = 0), Nil))
    else
      val pageSize    = context.pagination.pageSize
      val currentPage = context.pagination.currentPage
      val orderBy     = context.criteria.orderBy
      LeetCodeApi[IO](myLeetCodeDojo)
        .searchCompanyChallenges(
          pageSize.value * (currentPage - 1),
          pageSize.value,
          context.criteria.selectedInterviewPeriod.slug,
          context.criteria.selectedCompanies.map(_.slug).toList,
          context.criteria.selectedPositions.map(_.slug),
          context.criteria.selectedDifficulty,
          context.criteria.selectedStatus,
          context.criteria.selectedTags.map(_.userObj.asInstanceOf[LeetCodeTag]),
          orderBy
        )
        .map { response =>
          (
            context.pagination.copy(totalSize = response.total),
            response.questions.map { companyItem =>
              LeetCodeChallengeListItem(
                companyItem.acRate,
                companyItem.difficulty,
                companyItem.freqBar,
                companyItem.paidOnly,
                0,
                companyItem.status,
                companyItem.frontendQuestionId,
                companyItem.title,
                companyItem.titleCn,
                companyItem.titleSlug
              )
            }
          )
        }
  }

  override protected def saveQueryCriteria(queryCriteria: LeetCodeCompanyQueryCriteria, pagination: Pagination): Unit =
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

  override protected def loadQueryCriteria(): Option[(LeetCodeCompanyQueryCriteria, Pagination)] =
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
      .flatMap(value => decode[LeetCodeCompanyQueryCriteria](value).toOption)
      .zip(
        Option(queryCriteriaStore.get(s"${getClass.getSimpleName}-pageSize"))
          .flatMap(value => decode[PageSize](value).toOption)
          .map(v => Pagination(pageSize = v))
      )
      .map(identity)

  override def getQueryResultColumns: Array[OrderByColumnInfo[LeetCodeChallengeListItem, ?]] = {
    import LeetCodeTableColumnTitle.*
    val userIsPremium = myBoostrapParameters.userInfo.isPremium.getOrElse(false)
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
            setEnabled(!item.paidOnly || (item.paidOnly && userIsPremium))
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
      },
      new OrderByColumnInfo[LeetCodeChallengeListItem, String](Acceptance.title) {
        override def valueOf(item: LeetCodeChallengeListItem): String =
          myLeetCodeDojo match
            case CodeDojo.LeetCode   => f"${item.acRate}%.2f%%"
            case CodeDojo.LeetCodeCN => f"${item.acRate * 100}%.2f%%"

        override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
          }

        override def enableOrderBy: Boolean = true

        override def getOrderFilter: Option[OrderDirection] =
          getDirectionOf(LeetCodeSearchOrderBy.ACRate)

        override def setOrderFilter(filter: Option[OrderDirection]): Unit =
          setDirectionOf(LeetCodeSearchOrderBy.ACRate, filter)
      }
    )
  }
}

object LeetCodeCompanyQueryPresenter {
  private val COMPANY_TAG_RADIUS          = 0.1f
  private val POSITION_TAG_RADIUS         = 0.2f
  private val DIFFICULTY_TAG_RADIUS       = 0.3f
  private val STATUS_TAG_RADIUS           = 0.4f
  private val INTERVIEW_PERIOD_TAG_RADIUS = 0.5f
  private val TAG_TAG_RADIUS              = 1.0f

  case class LeetCodeCompanyQueryCriteria(
    selectedCompanies: Set[LeetCodeQuestionCompanyTag],
    selectedPositions: List[LeetCodeProblemsetPositionTag],
    selectedDifficulty: Option[ChallengeDifficulty],
    selectedStatus: Option[ChallengeStatus],
    selectedInterviewPeriod: InterviewPeriod,
    selectedTags: List[Tag],
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
  )

  object LeetCodeCompanyQueryCriteria {
    implicit val circeEncoder: Encoder[LeetCodeCompanyQueryCriteria] = Encoder.instance { criteria =>
      Json.obj(
        "selectedCompanies"       := criteria.selectedCompanies,
        "selectedPositions"       := criteria.selectedPositions,
        "selectedDifficulty"      := criteria.selectedDifficulty,
        "selectedStatus"          := criteria.selectedStatus,
        "selectedTags"            -> leetCodeTagToJson(criteria.selectedTags),
        "selectedInterviewPeriod" := criteria.selectedInterviewPeriod,
        "orderBy"                 := criteria.orderBy
      )
    }
    implicit val circeDecoder: Decoder[LeetCodeCompanyQueryCriteria] = Decoder.instance { cursor =>
      for {
        selectedCompanies       <- cursor.downField("selectedCompanies").as[Set[LeetCodeQuestionCompanyTag]]
        selectedPositions       <- cursor.downField("selectedPositions").as[List[LeetCodeProblemsetPositionTag]]
        selectedDifficulty      <- cursor.downField("selectedDifficulty").as[Option[ChallengeDifficulty]]
        selectedStatus          <- cursor.downField("selectedStatus").as[Option[ChallengeStatus]]
        selectedTags            <- cursor.downField("selectedTags").as[Json].flatMap(leetCodeTagFromJson)
        selectedInterviewPeriod <- cursor.downField("selectedInterviewPeriod").as[InterviewPeriod]
        orderBy                 <- cursor.downField("orderBy").as[Option[(LeetCodeSearchOrderBy, OrderDirection)]]
      } yield LeetCodeCompanyQueryCriteria(
        selectedCompanies,
        selectedPositions,
        selectedDifficulty,
        selectedStatus,
        selectedInterviewPeriod,
        selectedTags,
        orderBy
      )
    }
  }
}
