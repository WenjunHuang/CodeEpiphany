package com.wenjunhuang.codeepiphany.leetcode.ui

import cats.effect.IO
import javax.swing.{Icon, JTable, SwingConstants}
import javax.swing.table.{DefaultTableCellRenderer, TableCellRenderer}
import monocle.syntax.all.*

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup
import com.wenjunhuang.codeepiphany.leetcode.models.*
import com.wenjunhuang.codeepiphany.leetcode.services.{LeetCodeApi, LeetCodeSearchOrderBy}
import com.wenjunhuang.codeepiphany.leetcode.ui.LeetCodeKeywordQueryPresenter.QueryParams
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.{KeywordQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.implicits.*

class LeetCodeKeywordQueryPresenter(
  project: Project,
  bootstrap: LeetCodeBootstrapParameters,
  private val myLeetCodeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
) extends KeywordQueryPresenter[LeetCodeBootstrapParameters, QueryParams, LeetCodeChallengeListItem](
      project,
      bootstrap
    ) {
  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  override protected def createInitialQueryParameters(
    boostrapParameters: LeetCodeBootstrapParameters
  ): QueryContext[QueryParams] = QueryContext[QueryParams](criteria = QueryParams("", None), pagination = Pagination())

  override protected def executeQuery(
    context: QueryContext[QueryParams]
  ): IO[(Pagination, List[LeetCodeChallengeListItem])] = {
    val pageSize    = context.pagination.pageSize
    val currentPage = context.pagination.currentPage
    val keyword     = context.criteria.keyword
    val orderBy     = context.criteria.orderBy
    LeetCodeApi[IO](myLeetCodeDojo)
      .searchChallengesWithKeyword(pageSize.value * (currentPage - 1), pageSize.value, keyword, orderBy)
      .map { response =>
        (context.pagination.copy(totalSize = response.total), response.questions)
      }
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    super.uiDataSnapshot(dataSink)

    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createLeetCodeChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel, 
        myBoostrapParameters,
        myLeetCodeDojo)
    )
  }

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
              case ChallengeStatus.Solved   => AllIcons.General.GreenCheckmark
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
      },
      new OrderByColumnInfo[LeetCodeChallengeListItem, String](Frequency.title) {
        override def valueOf(item: LeetCodeChallengeListItem): String =
          item.freqBar match
            case None    => ""
            case Some(v) => f"${v}%.2f%%"

        override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
          }

        override def enableOrderBy: Boolean = true

        override def getOrderFilter: Option[OrderDirection] =
          getDirectionOf(LeetCodeSearchOrderBy.Frequency)

        override def setOrderFilter(filter: Option[OrderDirection]): Unit =
          setDirectionOf(LeetCodeSearchOrderBy.Frequency, filter)
      }
    )
  }

  override protected def updateQueryUI(context: QueryContext[QueryParams]): Unit = {}

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
}
object LeetCodeKeywordQueryPresenter {
  case class QueryParams(keyword: String, orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)])
  implicit val keywordContext: KeywordQueryPresenter.KeywordHolder[QueryParams] =
    new KeywordQueryPresenter.KeywordHolder[QueryParams] {
      override def keyword(v: QueryParams): String = v.keyword

      override def updateKeyword(v: QueryParams, keyword: String): QueryParams = v.copy(keyword = keyword)
    }
}
