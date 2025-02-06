package com.wenjunhuang.codeepiphany.hackerrank.ui

import cats.effect.IO
import cats.effect.implicits.*
import cats.syntax.all.*
import fs2.Stream
import javax.swing.{Icon, JTable, SwingConstants}
import javax.swing.table.{DefaultTableCellRenderer, TableCellRenderer}
import org.typelevel.ci.CIString

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.hackerrank.model.{HackerRankChallengeDetail, HackerRankContest}
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.{ChallengeDifficulty, ChallengeStatus}
import com.wenjunhuang.codeepiphany.services.{KeywordQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.Pagination

class HackerRankKeywordQueryPresenter(project: Project)
    extends KeywordQueryPresenter[Unit, HackerRankChallengeDetail](project, ()) {
  override protected def createInitialQueryParameters(boostrapParameters: Unit): QueryContext[String] =
    QueryContext[String](criteria = "", pagination = Pagination())

  override protected def executeQuery(
    context: QueryContext[String]
  ): IO[(Pagination, List[HackerRankChallengeDetail])] = {
    if context.criteria.isEmpty then IO.pure((context.pagination, Nil))
    else
      implicit val httpClientManager: HttpClientManager[IO] = HttpClientService.getInstance(myProject).httpClientManager
      val api                                               = HackerRankApi[IO]()
      val r = (
        api.searchChallengesWithKeyword(HackerRankContest.Master, context.criteria).recoverWith(_ => IO.pure(Nil)),
        api.searchChallengesWithKeyword(HackerRankContest.ProjectEuler, context.criteria).recoverWith(_ => IO.pure(Nil))
      ).parFoldMapA(identity)
      Stream
        .evals(r)
        .parEvalMapUnorderedUnbounded { case (contest, challenge) =>
          api.getChallengeDetail(challenge.challengeSlug, contest).attempt
        }
        .scan(Nil: List[HackerRankChallengeDetail]) {
          case (acc, Right(challenge)) => acc :+ challenge
          case (acc, _)                => acc
        }
        .compile
        .toList
        .map(challenges => (context.pagination, challenges.flatten))
  }

  override protected def getQueryResultColumns: Array[ColumnInfo[HackerRankChallengeDetail, ?]] = {
    import ColumnTitle.*
    Array(
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
  }

  override protected def updateQueryUI(context: QueryContext[String]): Unit = {}
}
