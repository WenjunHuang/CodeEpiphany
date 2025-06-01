package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode

import cats.effect.IO
import javax.swing.{Icon, JTable, SwingConstants}
import javax.swing.table.{DefaultTableCellRenderer, TableCellRenderer}
import javax.swing.GroupLayout.Alignment
import monocle.syntax.all.*
import scala.collection.mutable

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.project.Project
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.actions.TagsAction
import com.wenjunhuang.codeepiphany.actions.TagsAction.MultiTagGroupProvider
import com.wenjunhuang.codeepiphany.leetcode.models.{LeetCodeQuestionSolutionArticle, LeetCodeQuestionSolutionArticleAuthor, LeetCodeQuestionSolutionArticlesOrderBy, LeetCodeSolutionTags}
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.{Actions, CodeDojo}
import com.wenjunhuang.codeepiphany.services.{ParametersQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.LeetCodeSolutionArticlesPresenter.*
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.actions.ArticleOrderByAction
import com.wenjunhuang.codeepiphany.utils.{AsyncAvatarLoader, OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction
import com.wenjunhuang.codeepiphany.utils.PageSize.Twenty

class LeetCodeSolutionArticlesPresenter(
  project: Project,
  bootstrapParameters: BootstrapParameters,
  private val myCodeDojo: CodeDojo.LeetCodeCN.type | CodeDojo.LeetCode.type
) extends ParametersQueryPresenter[BootstrapParameters, QueryParams, LeetCodeQuestionSolutionArticle](
      project,
      bootstrapParameters
    ) {

  private implicit val httpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(myProject).httpClientManager

  private val myTabsMap = mutable.Map[String, TagsAction.Tag]()
  private val myTabs = List(
    TagsAction.TagGroupTab(
      "Tags",
      "tags",
      List(
        TagsAction.TagGroup(
          "Language",
          "language",
          myBoostrapParameters.tags.languageTags.map { it =>
            val tag = TagsAction.Tag(it.name, it.slug, "language", it)
            myTabsMap.put(tag.value, tag)
            tag
          },
          myBoostrapParameters.tags.languageTags
        ),
        TagsAction.TagGroup(
          "Knowledge",
          "Knowledge",
          myBoostrapParameters.tags.knowledgeTags.map { it =>
            val tag = TagsAction.Tag(it.name, it.slug, "difficulty", it)
            myTabsMap.put(tag.value, tag)
            tag
          },
          myBoostrapParameters.tags.knowledgeTags
        )
      ),
      "Select tags to filter articles"
    )
  )

  override protected def prepareProviders(
    getter: () => QueryContext[QueryParams],
    updater: (QueryContext[QueryParams] => QueryContext[QueryParams]) => Unit,
    dataSink: DataSink
  ): ActionGroup = {

    val tagProvider = new MultiTagGroupProvider {

      override def isSearchEnabled: Boolean = true

      override def searchTags(query: String): List[TagsAction.Tag] = Nil

      override def getTabs: List[TagsAction.TagGroupTab] = myTabs

      override def getAllItems: List[TagsAction.Tag] =
        myTabs.flatMap(_.tagGroups.flatMap(_.tags))

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: TagsAction.Tag): Boolean = getter().criteria.tagSlugs.contains(item.value)

      override def getSelectedItems: List[TagsAction.Tag] =
        getter().criteria.tagSlugs.map(slug => myTabsMap.get(slug)).collect { case Some(v) => v }

      override def addSelectedItems(items: List[TagsAction.Tag]): Unit = updater { old =>
        old.focus(_.criteria.tagSlugs).modify(it => (it ++ items.map(_.value)).distinct)
      }

      override def toggleSelection(item: TagsAction.Tag): Unit = updater { old =>
        if old.criteria.tagSlugs.contains(item.value) then
          old.focus(_.criteria.tagSlugs).modify(_.filterNot(_ == item.value))
        else old.focus(_.criteria.tagSlugs).modify(item.value +: _)
      }

      override def removeSelectedItems(items: List[TagsAction.Tag]): Unit = updater { old =>
        old.focus(_.criteria.tagSlugs).modify(_.filterNot(items.map(_.value).contains))
      }
    }

    val orderByProvider = new ArticleOrderByAction.ArticleOrderByParameterProvider {
      override def getAllItems: List[LeetCodeQuestionSolutionArticlesOrderBy] = myCodeDojo match {
        case CodeDojo.LeetCodeCN => LeetCodeQuestionSolutionArticlesOrderBy.values.toList
        case CodeDojo.LeetCode   => LeetCodeQuestionSolutionArticlesOrderBy.values.filter(_.leetCode.nonEmpty).toList
      }

      override def isMultipleSelection: Boolean = false

      override def isSelected(item: LeetCodeQuestionSolutionArticlesOrderBy): Boolean =
        getter().criteria.orderBy == item

      override def getSelectedItems: List[LeetCodeQuestionSolutionArticlesOrderBy] =
        List(getter().criteria.orderBy)

      override def addSelectedItems(items: List[LeetCodeQuestionSolutionArticlesOrderBy]): Unit =
        updater { old =>
          old.focus(_.criteria.orderBy).replace(items.headOption.getOrElse(LeetCodeQuestionSolutionArticlesOrderBy.Hot))
        }

      override def toggleSelection(item: LeetCodeQuestionSolutionArticlesOrderBy): Unit = {
        updater { old =>
          if old.criteria.orderBy == item then
            old.focus(_.criteria.orderBy).replace(LeetCodeQuestionSolutionArticlesOrderBy.Hot)
          else old.focus(_.criteria.orderBy).replace(item)
        }
      }

      override def removeSelectedItems(items: List[LeetCodeQuestionSolutionArticlesOrderBy]): Unit = {
        updater { old =>
          if items.contains(old.criteria.orderBy) then
            old.focus(_.criteria.orderBy).replace(LeetCodeQuestionSolutionArticlesOrderBy.Hot)
          else old
        }
      }
    }

    dataSink.set(TagsAction.TAG_PROVIDER_KEY, tagProvider)
    dataSink.set(ArticleOrderByAction.LEETCODE_ARTICLE_ORDERBY_KEY, orderByProvider)
    ActionManager.getInstance().getAction(Actions.LEETCODE_ARTICLES_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  }

  override protected def createQueryParametersTags(
    context: QueryContext[QueryParams],
    onCloseUpdater: (QueryContext[QueryParams] => QueryContext[QueryParams]) => Unit
  ): List[TagPaneAction] = {
    context.criteria.tagSlugs.map { slug =>
      myTabsMap.get(slug).map { tag =>
        TagPaneAction(
          tag.value,
          tag.name,
          None,
          0.5f,
          Some(
            { () => context.criteria.tagSlugs.contains(tag.value) },
            { selected =>
              if selected then onCloseUpdater(_.focus(_.criteria.tagSlugs).modify(tag.value +: _))
              else onCloseUpdater(_.focus(_.criteria.tagSlugs).modify(_.filterNot(_ == tag.value)))
            }
          ),
          None
        )
      }
    }.collect { case Some(v) => v }
  }

  override protected def createInitialQueryParameters(
    bootstrapParameters: BootstrapParameters
  ): QueryContext[QueryParams] =
    QueryContext(
      QueryParams(
        questionSlug = bootstrapParameters.questionSlug,
        userInput = None,
        orderBy = LeetCodeQuestionSolutionArticlesOrderBy.Hot,
        tagSlugs = List.empty
      ),
      Pagination(1, Twenty)
    )

  override protected def executeQuery(
    context: QueryContext[QueryParams]
  ): IO[(Pagination, List[LeetCodeQuestionSolutionArticle])] = {
    LeetCodeApi[IO](myCodeDojo)
      .searchQuestionSolutionArticles(
        context.pagination.offset,
        context.pagination.limit,
        context.criteria.questionSlug,
        context.criteria.orderBy,
        context.criteria.userInput,
        context.criteria.tagSlugs
      )
      .map { response =>
        (context.pagination.copy(totalSize = response.totalNum), response.edges.map(_.node))
      }
  }

  override def getQueryResultColumns: Array[OrderByColumnInfo[LeetCodeQuestionSolutionArticle, ?]] = Array(
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, LeetCodeQuestionSolutionArticleAuthor]("Author") {

      override def valueOf(item: LeetCodeQuestionSolutionArticle): LeetCodeQuestionSolutionArticleAuthor = item.author

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new IconTableCellRenderer[LeetCodeQuestionSolutionArticleAuthor]() {

          override def getIcon(value: LeetCodeQuestionSolutionArticleAuthor, table: JTable, row: Int): Icon = {
            value.avatarIcon.addListener(() => if (table != null) table.repaint())
            value.avatarIcon
          }

          override def isCenterAlignment: Boolean = false

          override def getText: String = item.author.username
        }
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, String]("Summary") {

      override def valueOf(item: LeetCodeQuestionSolutionArticle): String = item.summary.replaceAll("""\r\n|\n""", "")

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new DefaultTableCellRenderer()
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, String]("HitCount") {

      override def valueOf(item: LeetCodeQuestionSolutionArticle): String = s"${item.hitCount}"

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, String]("HitCount") {

      override def valueOf(item: LeetCodeQuestionSolutionArticle): String = s"${item.}"

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
        }
    },
  )
}

object LeetCodeSolutionArticlesPresenter {
  case class QueryParams(
    questionSlug: String,
    userInput: Option[String],
    orderBy: LeetCodeQuestionSolutionArticlesOrderBy,
    tagSlugs: List[String]
  )

  case class BootstrapParameters(questionSlug: String, tags: LeetCodeSolutionTags)

}
