package com.wenjunhuang.codeepiphany.controllers.dojo

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtil.repeat
import com.intellij.util.ui.table.IconTableCellRenderer
import com.intellij.util.ui.{ ColumnInfo, ListTableModel }
import com.wenjunhuang.codeepiphany.controllers.dojo.HackerRankChallengesTableModel.Column.*
import com.wenjunhuang.codeepiphany.hackerrank.model.{ ChallengeDifficulty, ChallengeListItem, ChallengeSkill, ChallengeStatus }
import org.typelevel.ci.CIString

import javax.swing.{ Icon, JTable, SwingConstants }
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }

class HackerRankChallengesTableModel extends ListTableModel[ChallengeListItem]() {

  setColumnInfos(
    Array(
      new ColumnInfo[ChallengeListItem, ChallengeStatus](Status.title) {
        override def valueOf(item: ChallengeListItem): ChallengeStatus = item.solved
          .map(b =>
            if b then ChallengeStatus.Solved
            else ChallengeStatus.Unsolved
          )
          .getOrElse(ChallengeStatus.Unsolved)

        override def getPreferredStringValue: String = Status.title

        override def getRenderer(item: ChallengeListItem): TableCellRenderer =
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
      new ColumnInfo[ChallengeListItem, String](Title.title) {
        override def valueOf(item: ChallengeListItem): String = item.name

        override def getPreferredStringValue: String = StringUtil.repeat("W", 30)
      },
      new ColumnInfo[ChallengeListItem, String](Difficulty.title) {
        override def valueOf(item: ChallengeListItem): String = ChallengeDifficulty.fromCIString(CIString(item.difficultyName)).map(_.showAsHtml).orNull
      },
      new ColumnInfo[ChallengeListItem, Int](MaxScore.title) {
        override def valueOf(item: ChallengeListItem): Int = item.maxScore

        override def getRenderer(item: ChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
          }

      },
      new ColumnInfo[ChallengeListItem, String](SuccessRate.title) {

        override def valueOf(item: ChallengeListItem): String = f"${item.successRatio * 100}%.2f%%"

        override def getRenderer(item: ChallengeListItem): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
          }

      }
    )
  )
}

object HackerRankChallengesTableModel {
  enum Column(val title: String) {
    case Status      extends Column("Status")
    case Title       extends Column("Title")
    case Difficulty  extends Column("Difficulty")
    case MaxScore    extends Column("Max Score")
    case SuccessRate extends Column("Success Rate")
  }

}
