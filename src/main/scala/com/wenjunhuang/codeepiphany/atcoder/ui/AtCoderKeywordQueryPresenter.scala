package com.wenjunhuang.codeepiphany.atcoder.ui

import cats.effect.IO
import javax.swing.{Icon, JTable}
import javax.swing.table.TableCellRenderer
import monocle.syntax.all.*
import org.jooq.impl.DSL
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup
import com.wenjunhuang.codeepiphany.atcoder.models.{AtCoderDifficulty, AtCoderSearchOrderBy}
import com.wenjunhuang.codeepiphany.atcoder.ui.AtCoderKeywordQueryPresenter.*
import com.wenjunhuang.codeepiphany.database.tables.records.AtcoderProblemsRecord
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.OrderDirection
import com.wenjunhuang.codeepiphany.services.{ChallengeRepository, KeywordQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.services.KeywordQueryPresenter.KeywordHolder
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink

class AtCoderKeywordQueryPresenter(project: Project, bootstrap: AtCoderBootstrapParameters)
    extends KeywordQueryPresenter[AtCoderBootstrapParameters, QueryParams, AtCoderTableItem](project, bootstrap) {
  override protected def createInitialQueryParameters(
    boostrapParameters: AtCoderBootstrapParameters
  ): QueryContext[QueryParams] = QueryContext[QueryParams](criteria = QueryParams("", None), pagination = Pagination())

  override protected def executeQuery(context: QueryContext[QueryParams]): IO[(Pagination, List[AtCoderTableItem])] = {
    if context.criteria.keyword.isEmpty then IO.pure((context.pagination, Nil))
    else
      ChallengeRepository
        .getInstance(myProject)
        .getDSLContextResource[IO]
        .use { dsl =>
          IO.delay {
            val keyword     = context.criteria.keyword
            val pageSize    = context.pagination.pageSize
            val currentPage = context.pagination.currentPage
            val total = dsl.selectCount
              .from(ATCODER_PROBLEMS)
              .innerJoin(ATCODER_PROBLEMS_FTS)
              .on(ATCODER_PROBLEMS.ID.eq(ATCODER_PROBLEMS_FTS.ID.cast(classOf[java.lang.Long])))
              .where(
                DSL
                  .condition("{0} MATCH {1}", DSL.field(ATCODER_PROBLEMS_FTS.getUnqualifiedName), s"\"${keyword}\"")
              )
              .fetchOne(0, classOf[Int])

            val base = dsl
              .select(ATCODER_PROBLEMS.fields()*)
              .from(ATCODER_PROBLEMS)
              .innerJoin(ATCODER_PROBLEMS_FTS)
              .on(ATCODER_PROBLEMS.ID.eq(ATCODER_PROBLEMS_FTS.ID.cast(classOf[java.lang.Long])))
              .where(
                DSL.condition("{0} MATCH {1}", DSL.field(ATCODER_PROBLEMS_FTS.getUnqualifiedName), s"\"${keyword}\"")
              )

            val orderBy = context.criteria.orderBy.map {
              case (AtCoderSearchOrderBy.ContestId, dir) => dir.toJooqSortField(ATCODER_PROBLEMS.CONTESTID)
              case (AtCoderSearchOrderBy.Difficulty, dir) =>
                dir.toJooqSortField(ATCODER_PROBLEMS.DIFFICULTY)
            }

            val query =
              orderBy match
                case None =>
                  base
                    .limit((currentPage - 1) * pageSize.value, pageSize.value)
                    .fetch()
                    .asScala
                    .map { record =>
                      AtCoderTableItem(record.into(classOf[AtcoderProblemsRecord]))
                    }
                    .toList
                case Some(orderBy) =>
                  base
                    .orderBy(orderBy)
                    .limit((currentPage - 1) * pageSize.value, pageSize.value)
                    .fetch()
                    .asScala
                    .map { record =>
                      AtCoderTableItem(record.into(classOf[AtcoderProblemsRecord]))
                    }
                    .toList
            (context.pagination.copy(totalSize = total), query)
          }
        }
  }

  override protected def updateQueryUI(context: QueryContext[QueryParams]): Unit = {}

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    super.uiDataSnapshot(dataSink)

    dataSink.set(
      OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY,
      createAtCoderChallengeProvider(myProject, myQueryResultSelectionModel, myQueryResultTableModel)
    )
  }

  def getDirectionOf(field: AtCoderSearchOrderBy): Option[OrderDirection] =
    myQueryStateManager.get.criteria.orderBy.collect {
      case (f, d) if f == field => d
    }

  def setDirectionOf(field: AtCoderSearchOrderBy, direction: Option[OrderDirection]): Unit = {
    direction match
      case None            => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(None))
      case Some(direction) => myQueryStateManager.update(_.focus(_.criteria.orderBy).replace(Some((field, direction))))
    requery(true)
  }

  override def getQueryResultColumns: Array[OrderByColumnInfo[AtCoderTableItem, ?]] = Array(
    new OrderByColumnInfo[AtCoderTableItem, String]("Id") {
      override def valueOf(item: AtCoderTableItem): String =
        item.record.getProblemid

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)
      override def enableOrderBy: Boolean          = false
    },
    new OrderByColumnInfo[AtCoderTableItem, String]("Title") {
      override def valueOf(item: AtCoderTableItem): String = item.record.getTitle

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)

      override def enableOrderBy: Boolean = false
    },
    new OrderByColumnInfo[AtCoderTableItem, Option[AtCoderDifficulty]]("Difficulty") {
      override def valueOf(item: AtCoderTableItem): Option[AtCoderDifficulty] =
        Option(item.record.getDifficulty)
          .map(AtCoderDifficulty.fromInt(_))

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(AtCoderSearchOrderBy.Difficulty)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(AtCoderSearchOrderBy.Difficulty, filter)

      override def getRenderer(item: AtCoderTableItem): TableCellRenderer = {
        new IconTableCellRenderer[Option[AtCoderDifficulty]]() {
          override def getIcon(value: Option[AtCoderDifficulty], table: JTable, row: Int): Icon =
            value match {
              case None =>
                setText("")
                null
              case Some(difficulty) =>
                setText(difficulty.showAsHtml(item.record.getDifficulty))
                ColorIcon(12, difficulty.color, true)
            }
        }
      }
    },
    new OrderByColumnInfo[AtCoderTableItem, String]("Contest Id") {
      override def valueOf(item: AtCoderTableItem): String = item.record.getContestid

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)

      override def enableOrderBy: Boolean = true
      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(AtCoderSearchOrderBy.ContestId)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(AtCoderSearchOrderBy.ContestId, filter)
    },
    new OrderByColumnInfo[AtCoderTableItem, String]("Contest Title") {
      override def valueOf(item: AtCoderTableItem): String = item.record.getContesttitle

      override def getPreferredStringValue: String = StringUtil.repeat("W", 10)

      override def enableOrderBy: Boolean = false
    }
  )
}

object AtCoderKeywordQueryPresenter {
  case class QueryParams(keyword: String, orderBy: Option[(AtCoderSearchOrderBy, OrderDirection)])
  implicit val keywordHolder: KeywordHolder[QueryParams] = new KeywordHolder[QueryParams] {
    override def keyword(v: QueryParams): String = v.keyword

    override def updateKeyword(v: QueryParams, keyword: String): QueryParams = v.copy(keyword = keyword)
  }
}
