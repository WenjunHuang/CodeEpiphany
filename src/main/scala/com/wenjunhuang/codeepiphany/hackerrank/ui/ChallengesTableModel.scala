package com.wenjunhuang.codeepiphany.hackerrank.ui

import javax.swing.{ Icon, JTable, ListSelectionModel, SwingConstants }
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }
import org.typelevel.ci.CIString

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager, DataSink, UiDataProvider }
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.table.TableView
import com.intellij.util.ui.{ ColumnInfo, ListTableModel }
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.hackerrank.model.HackerRankChallengeDetail
import com.wenjunhuang.codeepiphany.hackerrank.ui.ChallengesTableModel.*
import com.wenjunhuang.codeepiphany.hackerrank.ui.ChallengesTableModel.ColumnTitle.*
import com.wenjunhuang.codeepiphany.model.{ ChallengeDifficulty, ChallengeStatus }
import com.wenjunhuang.codeepiphany.model.Actions.*

class ChallengesTableModel extends ListTableModel[HackerRankChallengeDetail]() {

  private val myColumns: Array[ColumnInfo[HackerRankChallengeDetail, ?]] = Array(
    new ColumnInfo[HackerRankChallengeDetail, ChallengeStatus](Status.title) {
      override def valueOf(item: HackerRankChallengeDetail): ChallengeStatus = item.solved
        .map(b =>
          if b then ChallengeStatus.Solved
          else ChallengeStatus.Unsolved
        )
        .getOrElse(ChallengeStatus.Unsolved)

      override def getPreferredStringValue: String = Status.title

      override def getRenderer(item: HackerRankChallengeDetail): TableCellRenderer =
        new IconTableCellRenderer[ChallengeStatus]() {
          override def getIcon(value: ChallengeStatus, table: JTable, row: Int): Icon =
            value match {
              case ChallengeStatus.Solved => AllIcons.General.GreenCheckmark
              case _ =>
                if item.attempted.contains(true) then AllIcons.General.Modified
                else null
            }

          override def isCenterAlignment: Boolean = true

          override def getText: String = null
        }

    },
    new ColumnInfo[HackerRankChallengeDetail, String](Title.title) {
      override def valueOf(item: HackerRankChallengeDetail): String = item.name

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)
    },
    new ColumnInfo[HackerRankChallengeDetail, String](ColumnTitle.Difficulty.title) {
      override def valueOf(item: HackerRankChallengeDetail): String =
        ChallengeDifficulty.fromCIString(CIString(item.difficultyName)).map(_.showAsHtml).orNull
    },
    new ColumnInfo[HackerRankChallengeDetail, Int](MaxScore.title) {
      override def valueOf(item: HackerRankChallengeDetail): Int = item.maxScore

      override def getRenderer(item: HackerRankChallengeDetail): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }

    },
    new ColumnInfo[HackerRankChallengeDetail, String](SuccessRate.title) {

      override def valueOf(item: HackerRankChallengeDetail): String = f"${item.successRatio * 100}%.2f%%"

      override def getRenderer(item: HackerRankChallengeDetail): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }

    }
  )
  setColumnInfos(myColumns.asInstanceOf[Array[ColumnInfo[?, ?]]])

  def createTableView(): TableView[HackerRankChallengeDetail] = {
    val tableView = new TableView[HackerRankChallengeDetail](this)
    tableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    tableView.setShowGrid(false)
    tableView.setShowColumns(true)

    PopupHandler.installRowSelectionTablePopup(
      tableView,
      ActionManager.getInstance().getAction(CHALLENGES_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup],
      CHALLENGES_TABLE_POPUP_PLACE
    )
    tableView
  }
}

object ChallengesTableModel {
  enum ColumnTitle(val title: String) {
    case Status      extends ColumnTitle("Status")
    case Title       extends ColumnTitle("Title")
    case Difficulty  extends ColumnTitle("Difficulty")
    case MaxScore    extends ColumnTitle("Max Score")
    case SuccessRate extends ColumnTitle("Success Rate")
  }

}
