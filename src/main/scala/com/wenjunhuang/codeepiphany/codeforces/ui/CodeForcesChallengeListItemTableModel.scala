package com.wenjunhuang.codeepiphany.codeforces.ui

import java.awt.Color
import java.awt.event.{ MouseAdapter, MouseEvent }
import javax.swing.{ Icon, JTable, ListSelectionModel, SwingConstants }
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager }
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.table.TableView
import com.intellij.util.ui.{ ColumnInfo, JBUI, ListTableModel }
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesSearchOrderBy
import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesSearchOrderBy.ContestIdIndex
import com.wenjunhuang.codeepiphany.database.tables.CodeforcesProblemsets
import com.wenjunhuang.codeepiphany.database.tables.records.CodeforcesProblemsetsRecord
import com.wenjunhuang.codeepiphany.model.{ ChallengeStatus, CodeDojo, OrderDirection, OrderDirectionProvider }
import com.wenjunhuang.codeepiphany.model.Actions.*
import com.wenjunhuang.codeepiphany.utils.OrderByColumnInfo
import com.wenjunhuang.codeepiphany.utils.OrderByColumnInfo.nextOrderFilter

class CodeForcesChallengeListItemTableModel(
  private val myOrderProvider: OrderDirectionProvider[CodeForcesSearchOrderBy]
) extends ListTableModel[CodeforcesProblemsetsRecord]() {

  private val myColumns: Array[OrderByColumnInfo[CodeforcesProblemsetsRecord, ?]] = Array(
    new OrderByColumnInfo[CodeforcesProblemsetsRecord, String]("#") {
      override def valueOf(item: CodeforcesProblemsetsRecord): String =
        s"""${Option(item.getContestid).map(_.toString).getOrElse("")}${Option(item.getIndex).getOrElse("")}"""

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] = myOrderProvider.getDirectionOf(ContestIdIndex)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        myOrderProvider.setDirectionOf(ContestIdIndex, filter)
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
        myOrderProvider.getDirectionOf(CodeForcesSearchOrderBy.Rating)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        myOrderProvider.setDirectionOf(CodeForcesSearchOrderBy.Rating, filter)
    }
  )

  setColumnInfos(myColumns.asInstanceOf[Array[ColumnInfo[?, ?]]])

  def createTableView(): TableView[CodeforcesProblemsetsRecord] = {
    val tableView = new TableView[CodeforcesProblemsetsRecord](this)
    tableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
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

object CodeForcesChallengeListItemTableModel {
  enum ColumnTitle(val title: String) {
    case Status     extends ColumnTitle("Status")
    case Title      extends ColumnTitle("Title")
    case Solution   extends ColumnTitle("Solution")
    case Difficulty extends ColumnTitle("Difficulty")
    case Acceptance extends ColumnTitle("Acceptance")
    case Frequency  extends ColumnTitle("Frequency")
  }
}
