package com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode

import cats.effect.IO
import javax.swing.{ Icon, JTable, SwingConstants }
import javax.swing.table.{ DefaultTableCellRenderer, TableCellRenderer }
import javax.swing.event.{ ListSelectionEvent, ListSelectionListener }
import monocle.syntax.all.*
import scala.collection.mutable

import com.intellij.openapi.actionSystem.{ ActionGroup, ActionManager }
import com.intellij.openapi.project.Project
import com.intellij.util.ui.table.IconTableCellRenderer
import cats.syntax.all.*
import java.time.{ LocalDateTime, ZonedDateTime }
import java.time.format.DateTimeFormatter

import com.intellij.icons.AllIcons

import com.wenjunhuang.codeepiphany.actions.TagsAction
import com.wenjunhuang.codeepiphany.actions.TagsAction.MultiTagGroupProvider
import com.wenjunhuang.codeepiphany.leetcode.models.{
  LeetCodeQuestionSolutionArticle,
  LeetCodeQuestionSolutionArticleAuthor,
  LeetCodeQuestionSolutionArticlesOrderBy,
  LeetCodeSolutionTags,
  LeetCodeUserInfo
}
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.{ Actions, CodeDojo }
import com.wenjunhuang.codeepiphany.model.DifficultyColors.{ DIFFICULTY_EASY_COLOR, DIFFICULTY_MEDIUM_COLOR }
import com.wenjunhuang.codeepiphany.services.{ AuthService, ParametersQueryPresenter, QueryContext }
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.LeetCodeSolutionArticlesPresenter.*
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.solutions.leetcode.actions.ArticleOrderByAction
import com.wenjunhuang.codeepiphany.utils.{ AsyncAvatarLoader, OrderByColumnInfo, Pagination }
import com.wenjunhuang.codeepiphany.utils.actions.DataSink
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction
import com.wenjunhuang.codeepiphany.utils.PageSize.Twenty

class LeetCodeSolutionArticlesPresenter(
  project: Project,
  bootstrapParameters: BootstrapParameters,
  private val myOnSelected: (LeetCodeQuestionSolutionArticle) => Unit,
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
            val tag = TagsAction.Tag(it.nameTranslated.filter(_.nonEmpty).getOrElse(it.name), it.slug, "language", it)
            myTabsMap.put(tag.value, tag)
            tag
          },
          myBoostrapParameters.tags.languageTags
        ),
        TagsAction.TagGroup(
          "Knowledge",
          "Knowledge",
          myBoostrapParameters.tags.knowledgeTags.map { it =>
            val tag = TagsAction.Tag(it.nameTranslated.filter(_.nonEmpty).getOrElse(it.name), it.slug, "difficulty", it)
            myTabsMap.put(tag.value, tag)
            tag
          },
          myBoostrapParameters.tags.knowledgeTags
        )
      ),
      "Select tags to filter articles"
    )
  )

  private val mySelectionListener = new ListSelectionListener {
    override def valueChanged(e: ListSelectionEvent): Unit = {
      if (!e.getValueIsAdjusting && !getQueryResultTableSelectionModel.isSelectionEmpty) {
        val selectedIndex = getQueryResultTableSelectionModel.getMinSelectionIndex
        val selectedItem = getQueryResultTableModel.getItem(selectedIndex)
        myOnSelected(selectedItem)
      }
    }
  }

  getQueryResultTableSelectionModel.addListSelectionListener(mySelectionListener)

  override def dispose(): Unit = {
    getQueryResultTableSelectionModel.removeListSelectionListener(mySelectionListener)
    super.dispose()
  }

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
          None,
          Some { () => onCloseUpdater(_.focus(_.criteria.tagSlugs).modify(_.filterNot(_ == tag.value))) }
        )
      }
    }.collect { case Some(v) => v } :+ TagPaneAction(
      context.criteria.orderBy.toString,
      context.criteria.orderBy.show,
      None,
      0.7f,
      None,
      None
    )
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
      override def getPreferredStringValue: String = "Author"

      override def valueOf(item: LeetCodeQuestionSolutionArticle): LeetCodeQuestionSolutionArticleAuthor = item.author

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new IconTableCellRenderer[LeetCodeQuestionSolutionArticleAuthor]() {

          override def getIcon(value: LeetCodeQuestionSolutionArticleAuthor, table: JTable, row: Int): Icon = {
            value.avatarIcon.addListener(() => if (table != null) table.repaint())
            value.avatarIcon
          }

          override def isCenterAlignment: Boolean = false

          override def getText: String = item.author.realName

          setToolTipText(s"Author: ${item.author.realName}")
        }
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, Icon]("Status") {
      override def getPreferredStringValue: String = "St"
      override def valueOf(item: LeetCodeQuestionSolutionArticle): Icon = {
        if (item.chargeType.toUpperCase() == "PREMIUM") {
          if (myBoostrapParameters.userInfo.isPremium.contains(true)) {
            AllIcons.General.GreenCheckmark
          } else {
            AllIcons.Diff.Lock
          }
        } else {
          AllIcons.General.GreenCheckmark
        }
      }

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new IconTableCellRenderer[Icon]() {
          override def getIcon(value: Icon, table: JTable, row: Int): Icon = value

          override def isCenterAlignment: Boolean = true

          override def getText: String = ""

          setToolTipText(if (item.chargeType.toUpperCase() == "PREMIUM") "Premium only" else "Free")
        }
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, String]("Type") {
      override def getPreferredStringValue: String = "St"
      override def valueOf(item: LeetCodeQuestionSolutionArticle): String = {
        if (item.byLeetcode) {
          s"<html><font color='${DIFFICULTY_EASY_COLOR}'>O</font></html>"
        } else if (item.isEditorsPick.contains(true)) {
          s"<html><font color='${DIFFICULTY_MEDIUM_COLOR}'>EC</font></html>"
        } else {
          ""
        }
      }

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.CENTER)
          setToolTipText(
            if (item.byLeetcode) "Official Solution"
            else if (item.isEditorsPick.contains(true)) "Editor's Choice"
            else ""
          )
        }
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, String]("Summary") {
      override def getPreferredStringValue: String = "x".repeat(20)
      override def valueOf(item: LeetCodeQuestionSolutionArticle): String = item.summary.replaceAll("""\r\n|\n""", "")

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setToolTipText(item.summary)
        }
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, String]("HitCount") {
      override def getPreferredStringValue: String = "x".repeat(5)

      override def valueOf(item: LeetCodeQuestionSolutionArticle): String = s"${item.hitCount}"

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
          setToolTipText(s"Hit Count: ${item.hitCount}")
        }
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, String]("UpVote") {
      override def getPreferredStringValue: String = "x".repeat(5)
      override def valueOf(item: LeetCodeQuestionSolutionArticle): String = s"${item.upVote.getOrElse(0)}"

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new DefaultTableCellRenderer() {
          setHorizontalAlignment(SwingConstants.RIGHT)
          setToolTipText(s"UpVote count: ${item.upVote.getOrElse(0)}")
        }
    },
    new OrderByColumnInfo[LeetCodeQuestionSolutionArticle, String]("Created At") {
      override def getPreferredStringValue: String = "yyyy-MM-dd HH:mm:ss"

      override def valueOf(item: LeetCodeQuestionSolutionArticle): String =
        s"${ZonedDateTime.parse(item.createdAt).toLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}"

      override def getRenderer(item: LeetCodeQuestionSolutionArticle): TableCellRenderer =
        new DefaultTableCellRenderer()

    }
  )
}

object LeetCodeSolutionArticlesPresenter {
  case class QueryParams(
    questionSlug: String,
    userInput: Option[String],
    orderBy: LeetCodeQuestionSolutionArticlesOrderBy,
    tagSlugs: List[String]
  )

  case class BootstrapParameters(userInfo: LeetCodeUserInfo, questionSlug: String, tags: LeetCodeSolutionTags)

}
