package com.wenjunhuang.codeepiphany.codeforces.ui

import cats.effect.IO
import monocle.syntax.all.*
import org.jooq.impl.DSL
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup
import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesSearchOrderBy
import com.wenjunhuang.codeepiphany.codeforces.ui.CodeForcesKeywordQueryPresenter.*
import com.wenjunhuang.codeepiphany.database.tables.records.CodeforcesProblemsetsRecord
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.OrderDirection
import com.wenjunhuang.codeepiphany.services.{ ChallengeRepository, KeywordQueryPresenter, QueryContext }
import com.wenjunhuang.codeepiphany.services.KeywordQueryPresenter.KeywordHolder
import com.wenjunhuang.codeepiphany.utils.{ OrderByColumnInfo, PageSize, Pagination }
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import io.circe.generic.semiauto.*
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettings

class CodeForcesKeywordQueryPresenter(project: Project, bootstrap: CodeForcesBootstrapParameters)
    extends KeywordQueryPresenter[CodeForcesBootstrapParameters, QueryParams, CodeforcesProblemsetsRecord](
      project,
      bootstrap
    ) {
  override protected def createInitialQueryParameters(
    boostrapParameters: CodeForcesBootstrapParameters
  ): QueryContext[QueryParams] = QueryContext[QueryParams](criteria = QueryParams("", None), pagination = Pagination())

  override protected def executeQuery(
    context: QueryContext[QueryParams]
  ): IO[(Pagination, List[CodeforcesProblemsetsRecord])] = {
    if context.criteria.keyword.isEmpty then IO.pure((context.pagination, Nil))
    else
      ChallengeRepository
        .getInstance(myProject)
        .getDSLContextResource[IO]
        .use { dsl =>
          IO.delay {
            val keyword     = context.criteria.keyword
            val orderBy     = context.criteria.orderBy
            val pageSize    = context.pagination.pageSize
            val currentPage = context.pagination.currentPage
            val total = dsl.selectCount
              .from(CODEFORCES_PROBLEMSETS_FTS)
              .where(
                DSL
                  .condition(
                    "{0} MATCH {1}",
                    DSL.field(CODEFORCES_PROBLEMSETS_FTS.getUnqualifiedName),
                    s"\"${keyword}\""
                  )
              )
              .fetchOne(0, classOf[Int])

            val base = dsl
              .selectFrom(CODEFORCES_PROBLEMSETS_FTS)
              .where(
                DSL
                  .condition(
                    "{0} MATCH {1}",
                    DSL.field(CODEFORCES_PROBLEMSETS_FTS.getUnqualifiedName),
                    s"\"${keyword}\""
                  )
              )

            (
              context.pagination.copy(totalSize = total),
              orderBy match
                case None =>
                  base
                    .limit(pageSize.value)
                    .offset((currentPage - 1) * pageSize.value)
                    .fetchInto(classOf[CodeforcesProblemsetsRecord])
                    .asScala
                    .toList
                case Some(order) =>
                  (order match
                    case (CodeForcesSearchOrderBy.ContestIdIndex, dir) =>
                      base.orderBy(dir.toJooqSortField(CODEFORCES_PROBLEMSETS_FTS.CONTESTIDINDEX))
                    case (CodeForcesSearchOrderBy.Rating, dir) =>
                      base.orderBy(dir.toJooqSortField(CODEFORCES_PROBLEMSETS_FTS.RATING))
                  )
                    .limit(pageSize.value)
                    .offset((currentPage - 1) * pageSize.value)
                    .fetchInto(classOf[CodeforcesProblemsetsRecord])
                    .asScala
                    .toList
            )
          }
        }
  }

  private def getDirectionOf(field: CodeForcesSearchOrderBy): Option[OrderDirection] =
    myQueryStateManager.get.criteria.orderBy.collect {
      case (f, d) if f == field => d
    }

  private def setDirectionOf(field: CodeForcesSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    direction match
      case None            => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(None))
      case Some(direction) => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(Some((field, direction))))
    requery(true)
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    super.uiDataSnapshot(dataSink)

    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createCodeForcesChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel)
    )
  }

  override def getQueryResultColumns: Array[OrderByColumnInfo[CodeforcesProblemsetsRecord, ?]] = Array(
    new OrderByColumnInfo[CodeforcesProblemsetsRecord, String]("#") {
      override def valueOf(item: CodeforcesProblemsetsRecord): String =
        s"""${Option(item.getContestid).map(_.toString).getOrElse("")}${Option(item.getIndex).getOrElse("")}"""

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] = getDirectionOf(CodeForcesSearchOrderBy.ContestIdIndex)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(CodeForcesSearchOrderBy.ContestIdIndex, filter)
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
        getDirectionOf(CodeForcesSearchOrderBy.Rating)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(CodeForcesSearchOrderBy.Rating, filter)
    }
  )

  override protected def saveQueryCriteria(queryCriteria: QueryParams, pagination: Pagination): Unit =
    val storage = CodeForcesSettings.getInstance(myProject).getState.queryCriteria
    storage.put(s"${getClass.getSimpleName}-criteria", queryCriteria.asJson.noSpaces)
    storage.put(s"${getClass.getSimpleName}-pageSize", pagination.pageSize.value.toString)

  override protected def loadQueryCriteria(): Option[(QueryParams, Pagination)] =
    val storage = CodeForcesSettings.getInstance(myProject).getState.queryCriteria
    Option(storage.get(s"${getClass.getSimpleName}-criteria"))
      .flatMap(decode[QueryParams](_).toOption)
      .zip(
        Option(storage.get(s"${getClass.getSimpleName}-pageSize"))
          .flatMap(s => decode[PageSize](s).toOption)
          .map(v => Pagination(pageSize = v))
      )

}

object CodeForcesKeywordQueryPresenter {
  case class QueryParams(keyword: String, orderBy: Option[(CodeForcesSearchOrderBy, OrderDirection)])
  object QueryParams {
    implicit val circeEncoder: Encoder[QueryParams] = deriveEncoder[QueryParams]
    implicit val circeDecoder: Decoder[QueryParams] = deriveDecoder[QueryParams]
  }
  implicit val keywordHolder: KeywordHolder[QueryParams] = new KeywordHolder[QueryParams] {
    override def keyword(v: QueryParams): String = v.keyword

    override def updateKeyword(v: QueryParams, keyword: String): QueryParams = v.copy(keyword = keyword)
  }
}
