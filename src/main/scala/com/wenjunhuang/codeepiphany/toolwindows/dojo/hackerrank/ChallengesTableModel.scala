package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink, UiDataProvider}
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.PopupHandler
import com.intellij.ui.table.TableView
import com.intellij.util.ui.table.IconTableCellRenderer
import com.intellij.util.ui.{ColumnInfo, ListTableModel}
import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeDetail
import com.wenjunhuang.codeepiphany.model.Actions.*
import com.wenjunhuang.codeepiphany.model.{ChallengeDifficulty, ChallengeStatus}
import com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.ChallengesTableModel.COLUMNS
import com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.ChallengesTableModel.ColumnTitle.*
import org.typelevel.ci.CIString

import javax.swing.table.{DefaultTableCellRenderer, TableCellRenderer}
import javax.swing.{Icon, JTable, ListSelectionModel, SwingConstants}

class ChallengesTableModel extends ListTableModel[ChallengeDetail]() {
  setColumnInfos(COLUMNS.asInstanceOf[Array[ColumnInfo[?, ?]]])

  def createTableView(setDataSink: DataSink => Unit): TableView[ChallengeDetail] = {
    val tableView = new TableView(this) with UiDataProvider {
      override def uiDataSnapshot(dataSink: DataSink): Unit =
        setDataSink(dataSink)
    }

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

  val COLUMNS: Array[ColumnInfo[ChallengeDetail, ?]] = Array(
    new ColumnInfo[ChallengeDetail, ChallengeStatus](Status.title) {
      override def valueOf(item: ChallengeDetail): ChallengeStatus = item.solved
        .map(b =>
          if b then ChallengeStatus.Solved
          else ChallengeStatus.Unsolved
        )
        .getOrElse(ChallengeStatus.Unsolved)

      override def getPreferredStringValue: String = Status.title

      override def getRenderer(item: ChallengeDetail): TableCellRenderer =
        new IconTableCellRenderer[ChallengeStatus]() {
          override def getIcon(value: ChallengeStatus, table: JTable, row: Int): Icon =
            value match {
              case ChallengeStatus.Solved => AllIcons.General.GreenCheckmark
              case ChallengeStatus.Unsolved =>
                if item.attempted.contains(true) then AllIcons.General.Modified
                else null
            }

          override def isCenterAlignment: Boolean = true

          override def getText: String = null
        }

    },
    new ColumnInfo[ChallengeDetail, String](Title.title) {
      override def valueOf(item: ChallengeDetail): String = item.name

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)
    },
    new ColumnInfo[ChallengeDetail, String](ColumnTitle.Difficulty.title) {
      override def valueOf(item: ChallengeDetail): String =
        ChallengeDifficulty.fromCIString(CIString(item.difficultyName)).map(_.showAsHtml).orNull
    },
    new ColumnInfo[ChallengeDetail, Int](MaxScore.title) {
      override def valueOf(item: ChallengeDetail): Int = item.maxScore

      override def getRenderer(item: ChallengeDetail): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }

    },
    new ColumnInfo[ChallengeDetail, String](SuccessRate.title) {

      override def valueOf(item: ChallengeDetail): String = f"${item.successRatio * 100}%.2f%%"

      override def getRenderer(item: ChallengeDetail): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }

    }
  )
}
