package com.wenjunhuang.codeepiphany.controllers.dojo.hackerrank

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtil.repeat
import com.intellij.util.ui.table.IconTableCellRenderer
import com.intellij.util.ui.{ ColumnInfo, ListTableModel }
import com.wenjunhuang.codeepiphany.hackerrank.model.{ ChallengeDetail, ChallengeDifficulty, ChallengeSkill, ChallengeStatus }
import org.typelevel.ci.CIString

import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }
import javax.swing.{ Icon, JTable, SwingConstants }
import ChallengesTableModel.Column.*

class ChallengesTableModel extends ListTableModel[ChallengeDetail]() {

  setColumnInfos(
    Array(
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
      new ColumnInfo[ChallengeDetail, String](Difficulty.title) {
        override def valueOf(item: ChallengeDetail): String = ChallengeDifficulty.fromCIString(CIString(item.difficultyName)).map(_.showAsHtml).orNull
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
  )
}

object ChallengesTableModel {
  enum Column(val title: String) {
    case Status      extends Column("Status")
    case Title       extends Column("Title")
    case Difficulty  extends Column("Difficulty")
    case MaxScore    extends Column("Max Score")
    case SuccessRate extends Column("Success Rate")
  }
}
