package com.wenjunhuang.codeepiphany.leetcode.ui

import java.awt.Color
import java.awt.event.{ MouseAdapter, MouseEvent }
import javax.swing.{ Icon, JTable, ListSelectionModel, SwingConstants }
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.table.TableView
import com.intellij.util.ui.{ ColumnInfo, JBUI, ListTableModel }
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.leetcode.model.{
  fromLeetCodeDifficulty,
  fromLeetCodeStatus,
  LeetCodeChallengeListItem
}
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeSearchOrderBy
import com.wenjunhuang.codeepiphany.leetcode.ui.LeetCodeChallengeListItemTableModel.ColumnTitle.*
import com.wenjunhuang.codeepiphany.model.{ ChallengeStatus, CodeDojo, OrderDirection, OrderDirectionProvider }
import com.wenjunhuang.codeepiphany.model.Actions.*
import com.wenjunhuang.codeepiphany.utils.OrderByColumnInfo
import com.wenjunhuang.codeepiphany.utils.OrderByColumnInfo.nextOrderFilter

class LeetCodeChallengeListItemTableModel(
  private val myOrderProvider: OrderDirectionProvider[LeetCodeSearchOrderBy],
  private val myCodeDojo: CodeDojo
) extends ListTableModel[LeetCodeChallengeListItem]() {

  private val myColumns: Array[OrderByColumnInfo[LeetCodeChallengeListItem, ?]] = Array(
    new OrderByColumnInfo[LeetCodeChallengeListItem, ChallengeStatus](Status.title) {
      override def valueOf(item: LeetCodeChallengeListItem): ChallengeStatus = item.status
        .map(myCodeDojo.fromLeetCodeStatus)
        .getOrElse(ChallengeStatus.Unsolved)

      override def getPreferredStringValue: String = Status.title

      override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
        new IconTableCellRenderer[ChallengeStatus]() {
          override def getIcon(value: ChallengeStatus, table: JTable, row: Int): Icon =
            value match
              case ChallengeStatus.Solved   => AllIcons.General.GreenCheckmark
              case ChallengeStatus.Tried    => AllIcons.General.Modified
              case ChallengeStatus.Unsolved => null

          override def isCenterAlignment: Boolean = true

          override def getText: String = null
        }
    },
    new OrderByColumnInfo[LeetCodeChallengeListItem, String](Title.title) {
      override def valueOf(item: LeetCodeChallengeListItem): String =
        s"[${item.frontendQuestionId}]${item.titleCn.filter(_.nonEmpty).getOrElse(item.title)}"

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        myOrderProvider.getDirectionOf(LeetCodeSearchOrderBy.FontEndId)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit = {
        myOrderProvider.setDirectionOf(LeetCodeSearchOrderBy.FontEndId, filter)
      }
    },
    new OrderByColumnInfo[LeetCodeChallengeListItem, String](Solution.title) {
      override def valueOf(item: LeetCodeChallengeListItem): String = item.solutionNum.toString

      override def getPreferredStringValue: String = Solution.title

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        myOrderProvider.getDirectionOf(LeetCodeSearchOrderBy.SolutionNum)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit = {
        myOrderProvider.setDirectionOf(LeetCodeSearchOrderBy.SolutionNum, filter)
      }
    },
    new OrderByColumnInfo[LeetCodeChallengeListItem, String](Difficulty.title) {
      override def valueOf(item: LeetCodeChallengeListItem): String =
        myCodeDojo.fromLeetCodeDifficulty(item.difficulty).showAsHtml

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        myOrderProvider.getDirectionOf(LeetCodeSearchOrderBy.Difficulty)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        myOrderProvider.setDirectionOf(LeetCodeSearchOrderBy.Difficulty, filter)
    },
    new OrderByColumnInfo[LeetCodeChallengeListItem, String](Acceptance.title) {
      override def valueOf(item: LeetCodeChallengeListItem): String =
        myCodeDojo match
          case CodeDojo.LeetCode   => f"${item.acRate}%.2f%%"
          case CodeDojo.LeetCodeCN => f"${item.acRate * 100}%.2f%%"
          case _                   => ""

      override def getRenderer(item: LeetCodeChallengeListItem): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] = myOrderProvider.getDirectionOf(LeetCodeSearchOrderBy.ACRate)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        myOrderProvider.setDirectionOf(LeetCodeSearchOrderBy.ACRate, filter)
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
        myOrderProvider.getDirectionOf(LeetCodeSearchOrderBy.Frequency)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        myOrderProvider.setDirectionOf(LeetCodeSearchOrderBy.Frequency, filter)
    }
  )

  setColumnInfos(myColumns.asInstanceOf[Array[ColumnInfo[?, ?]]])

  def createTableView(setDataSink: DataSink => Unit): TableView[LeetCodeChallengeListItem] = {
    val tableView = new TableView(this) with UiDataProvider {
      override def uiDataSnapshot(dataSink: DataSink): Unit = setDataSink(dataSink)
    }
    tableView.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
    tableView.setShowGrid(false)
    tableView.setShowColumns(true)
    tableView.getTableHeader.setDefaultRenderer(
      (table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int) => {
        val nameLabel = DefaultTableCellRenderer()
        nameLabel.setBorder(JBUI.Borders.empty(0, 4, 0, 0))
        nameLabel.setText(value.asInstanceOf[String])
        val panel = BorderLayoutPanel()
        panel.setBackground(new Color(0, 0, 0, 0))
        panel.addToCenter(nameLabel)

        if myColumns(column).enableOrderBy then
          val sortIcon = DefaultTableCellRenderer()
          myColumns(column).getOrderFilter match
            case None                            => sortIcon.setIcon(AllIcons.General.ArrowSplitCenterV)
            case Some(OrderDirection.Ascending)  => sortIcon.setIcon(AllIcons.General.ArrowUp)
            case Some(OrderDirection.Descending) => sortIcon.setIcon(AllIcons.General.ArrowDown)
          panel.addToRight(sortIcon)
        else panel
      }
    )
    tableView.getTableHeader.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        val columnIndex = tableView.columnAtPoint(e.getPoint)
        if columnIndex >= 0 then
          val columnInfo = myColumns(columnIndex)
          if columnInfo.enableOrderBy then columnInfo.setOrderFilter(nextOrderFilter(columnInfo.getOrderFilter))
          tableView.getTableHeader.revalidate()
      }
    })

    PopupHandler.installRowSelectionTablePopup(
      tableView,
      ActionManager.getInstance().getAction(CHALLENGES_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup],
      CHALLENGES_TABLE_POPUP_PLACE
    )
    tableView
  }
}

object LeetCodeChallengeListItemTableModel {
  enum ColumnTitle(val title: String) {
    case Status     extends ColumnTitle("Status")
    case Title      extends ColumnTitle("Title")
    case Solution   extends ColumnTitle("Solution")
    case Difficulty extends ColumnTitle("Difficulty")
    case Acceptance extends ColumnTitle("Acceptance")
    case Frequency  extends ColumnTitle("Frequency")
  }
}
