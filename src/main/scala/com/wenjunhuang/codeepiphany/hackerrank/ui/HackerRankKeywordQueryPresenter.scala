package com.wenjunhuang.codeepiphany.hackerrank.ui

import cats.effect.IO
import cats.effect.implicits.*
import cats.syntax.all.*
import fs2.Stream
import javax.swing.{ Icon, JTable, SwingConstants }
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }
import org.typelevel.ci.CIString

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.hackerrank.models.{ HackerRankChallengeDetail, HackerRankContest }
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.ui.HackerRankKeywordQueryPresenter.QueryParams
import com.wenjunhuang.codeepiphany.model.{ ChallengeDifficulty, ChallengeStatus }
import com.wenjunhuang.codeepiphany.services.{ KeywordQueryPresenter, QueryContext }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.utils.{ OrderByColumnInfo, PageSize, Pagination }
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.parser.*

import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings

class HackerRankKeywordQueryPresenter(project: Project)
    extends KeywordQueryPresenter[Unit, QueryParams, HackerRankChallengeDetail](project, ()) {
  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  override protected def createInitialQueryParameters(boostrapParameters: Unit): QueryContext[QueryParams] =
    QueryContext[QueryParams](criteria = QueryParams(""), pagination = Pagination())

  override protected def executeQuery(
    context: QueryContext[QueryParams]
  ): IO[(Pagination, List[HackerRankChallengeDetail])] = {
    if context.criteria.keyword.isEmpty then IO.pure((context.pagination, Nil))
    else
      val api = HackerRankApi[IO]()
      val r = (
        api
          .searchChallengesWithKeyword(HackerRankContest.Master, context.criteria.keyword)
          .recoverWith(_ => IO.pure(Nil)),
        api
          .searchChallengesWithKeyword(HackerRankContest.ProjectEuler, context.criteria.keyword)
          .recoverWith(_ => IO.pure(Nil))
      ).parMapN(_ ++ _)
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

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    super.uiDataSnapshot(dataSink)

    dataSink.set(
      CHALLENGE_PROVIDER_KEY,
      createHackerRankChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel)
    )
  }

  override def getQueryResultColumns: Array[OrderByColumnInfo[HackerRankChallengeDetail, ?]] = {
    import HackerRankTableColumnTitle.*
    Array(
      new OrderByColumnInfo[HackerRankChallengeDetail, ChallengeStatus](Status.title) {
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
                case ChallengeStatus.Solved => AllIcons.General.InspectionsOK
                case _ =>
                  if item.attempted.contains(true) then AllIcons.General.Modified
                  else null
              }

            override def isCenterAlignment: Boolean = true

            override def getText: String = null
          }

      },
      new OrderByColumnInfo[HackerRankChallengeDetail, String](Title.title) {
        override def valueOf(item: HackerRankChallengeDetail): String = item.name

        override def getPreferredStringValue: String = StringUtil.repeat("W", 30)
      },
      new OrderByColumnInfo[HackerRankChallengeDetail, String](HackerRankTableColumnTitle.Difficulty.title) {
        override def valueOf(item: HackerRankChallengeDetail): String =
          ChallengeDifficulty.fromCIString(CIString(item.difficultyName)).map(_.showAsHtml).orNull
      },
      new OrderByColumnInfo[HackerRankChallengeDetail, Int](MaxScore.title) {
        override def valueOf(item: HackerRankChallengeDetail): Int = item.maxScore

        override def getRenderer(item: HackerRankChallengeDetail): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
          }

      },
      new OrderByColumnInfo[HackerRankChallengeDetail, String](SuccessRate.title) {

        override def valueOf(item: HackerRankChallengeDetail): String = f"${item.successRatio * 100}%.2f%%"

        override def getRenderer(item: HackerRankChallengeDetail): TableCellRenderer =
          new DefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT)
          }
      }
    )
  }

  override protected def saveQueryCriteria(queryCriteria: QueryParams, pagination: Pagination): Unit =
    val storage = HackerRankSettings.getInstance(myProject).getState.queryCriteria
    storage.put(s"${getClass.getSimpleName}-criteria", queryCriteria.asJson.noSpaces)
    storage.put(s"${getClass.getSimpleName}-pageSize", pagination.pageSize.value.toString)

  override protected def loadQueryCriteria(): Option[(QueryParams, Pagination)] =
    val storage = HackerRankSettings.getInstance(myProject).getState.queryCriteria
    Option(storage.get(s"${getClass.getSimpleName}-criteria"))
      .flatMap(value => decode[QueryParams](value).toOption)
      .zip(
        Option(storage.get(s"${getClass.getSimpleName}-pageSize"))
          .flatMap(value => decode[PageSize](value).toOption)
          .map(value => Pagination(pageSize = value))
      )

}
object HackerRankKeywordQueryPresenter {
  case class QueryParams(keyword: String)
  implicit val keywordContext: KeywordQueryPresenter.KeywordHolder[QueryParams] =
    new KeywordQueryPresenter.KeywordHolder[QueryParams] {
      override def keyword(v: QueryParams): String = v.keyword

      override def updateKeyword(v: QueryParams, keyword: String): QueryParams = v.copy(keyword = keyword)
    }
}
