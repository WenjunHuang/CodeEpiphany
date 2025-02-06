package com.wenjunhuang.codeepiphany.hackerrank.ui

import cats.effect.IO
import cats.syntax.all.*
import javax.swing.{Icon, JTable, SwingConstants}
import javax.swing.table.{DefaultTableCellRenderer, TableCellRenderer}
import monocle.syntax.all.*
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.actions.{DifficultyParameterAction, OpenChallengeActionGroup, StatusParameterAction, TagsAction}
import com.wenjunhuang.codeepiphany.actions.DifficultyParameterAction.DifficultyParameterProvider
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.OpenChallengeProvider
import com.wenjunhuang.codeepiphany.actions.StatusParameterAction.StatusParameterProvider
import com.wenjunhuang.codeepiphany.actions.TagsAction.{SingleTagGroupProvider, Tag}
import com.wenjunhuang.codeepiphany.hackerrank.actions.{CategoryParameterAction, SkillParameterAction}
import com.wenjunhuang.codeepiphany.hackerrank.actions.CategoryParameterAction.{Category, CategoryProvider}
import com.wenjunhuang.codeepiphany.hackerrank.actions.SkillParameterAction.SkillParameterProvider
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.hackerrank.services.{HackerRankApi, HackerRankOpenChallengeRequest, HackerRankOpenChallengeService}
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.hackerrank.ui.ColumnTitle
import com.wenjunhuang.codeepiphany.hackerrank.ui.HackerRankQueryParametersPresenter.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.{console, ParametersQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.Pagination
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction

class HackerRankQueryParametersPresenter(project: Project, bootstraps: HackerRankBootstrapParameters)
    extends ParametersQueryPresenter[HackerRankBootstrapParameters, QueryParams, HackerRankChallengeDetail](project, bootstraps) {
  private implicit val myHttpClientManager: HttpClientManager[IO] =
    HttpClientService.getInstance(project).httpClientManager

  private val myLogger = LoggerFactory.getLogger[IO]

  override protected def createInitialQueryParameters(boostrapParameters: HackerRankBootstrapParameters): QueryContext[QueryParams] = {
    QueryContext[QueryParams](QueryParams(boostrapParameters.challengeDomains.head, Nil, Nil, None, Nil), Pagination())
  }

  override protected def prepareProviders(
    getter: () => QueryContext[QueryParams],
    updater: (QueryContext[QueryParams] => QueryContext[QueryParams]) => Unit,
    dataSink: DataSink
  ): ActionGroup = {
    val myChallengeProvider = new OpenChallengeProvider {
      override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
        myQueryResultSelectionModel.getSelectedIndices.toList match
          case first :: tail =>
            val selected = myQueryResultTableModel.getItem(first)
            HackerRankOpenChallengeService[IO](myProject)
              .openChallenge(
                HackerRankOpenChallengeRequest(
                  selected.slug,
                  HackerRankContest.fromCIString(CIString(selected.contestSlug)).get
                ),
                language,
                languageVersion
              )
              .handleErrorWith(e =>
                console.error[IO](myProject, e.getMessage) *> myLogger.warn(e)(
                  s"Failed to open challenge ${selected.slug}"
                )
              )
              .evalAsBackgroundProgress(myProject, s"Opening HackerRank challenge '${selected.name}'...")
              .unsafeRunAndForget()
          case _ => ()
      }

      override def getLanguages: List[(Language, LanguageVersion)] = {
        val settings = HackerRankSettings.getInstance(myProject)
        settings.getSelectedLanguages
      }

      override def currentSelectedCanBeOpened: Boolean = true
    }

    val myCategoryProvider = new CategoryProvider {
      override def isSelected(item: Category): Boolean =
        getter().criteria.selectedDomain.slug == item.value

      override def toggleSelection(item: Category): Unit =
        val myState = getter().criteria
        if myState.selectedDomain.slug != item.value && myBoostrapParameters.challengeDomains.exists(
            _.slug == item.value
          )
        then
          updater { oldContext =>
            myBoostrapParameters.challengeDomains
              .find(_.slug == item.value)
              .map { newSelected =>
                oldContext
                  .copy(criteria = oldContext.criteria.copy(selectedDomain = newSelected, selectedSubdomains = Nil))
                  .resetPagination
              }
              .getOrElse(oldContext)
          }

      override def isMultipleSelection: Boolean = false

      override def getAllItems: List[Category] =
        myBoostrapParameters.challengeDomains.map(domain => Category(domain.name, domain.slug))

      override def getSelectedItems: List[Category] = {
        val myState = getter().criteria
        List(Category(myState.selectedDomain.name, myState.selectedDomain.slug))
      }

      override def addSelectedItems(items: List[Category]): Unit =
        myBoostrapParameters.challengeDomains.find(_.slug == items.head.value) match
          case Some(newSelected) =>
            updater { old =>
              if old.criteria.selectedDomain.slug != newSelected.slug then
                old.copy(criteria = old.criteria.copy(selectedDomain = newSelected, selectedSubdomains = Nil))
              else old
            }
          case _ =>

      override def removeSelectedItems(items: List[Category]): Unit = {}
    }

    val myDifficultiesProvider = new DifficultyParameterProvider {
      override def getAllItems: List[ChallengeDifficulty] =
        List(ChallengeDifficulty.Easy, ChallengeDifficulty.Medium, ChallengeDifficulty.Hard)

      override def isSelected(item: ChallengeDifficulty): Boolean =
        getter().criteria.selectedDifficulties.contains(item)

      override def isMultipleSelection: Boolean = true

      override def toggleSelection(item: ChallengeDifficulty): Unit = {
        updater { old =>
          val myState = old.criteria
          if myState.selectedDifficulties.contains(item) then
            old.copy(criteria =
              old.criteria.copy(selectedDifficulties = myState.selectedDifficulties.filterNot(_ == item))
            )
          else old.copy(criteria = old.criteria.copy(selectedDifficulties = myState.selectedDifficulties :+ item))
        }
      }

      override def getSelectedItems: List[ChallengeDifficulty] = getter().criteria.selectedDifficulties

      override def addSelectedItems(items: List[ChallengeDifficulty]): Unit = {
        updater { old =>
          old.copy(criteria =
            old.criteria.copy(selectedDifficulties = (old.criteria.selectedDifficulties ++ items).distinct)
          )
        }
      }

      override def removeSelectedItems(items: List[ChallengeDifficulty]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedDifficulties).modify(_.filterNot(items.contains))
        }
      }
    }

    val myStatusProvider = new StatusParameterProvider {
      private val allItems = List(ChallengeStatus.Solved, ChallengeStatus.Unsolved)

      override def isSelected(item: ChallengeStatus): Boolean =
        getter().criteria.selectedStatus.exists(_.value == item.value)

      override def getAllItems: List[ChallengeStatus] = allItems

      override def isMultipleSelection: Boolean = false

      override def getSelectedItems: List[ChallengeStatus] = getter().criteria.selectedStatus.toList

      override def addSelectedItems(items: List[ChallengeStatus]): Unit = {
        updater { old =>
          old
            .focus(_.criteria.selectedStatus)
            .replace(items.headOption.flatMap(status => allItems.find(_.value == status.value)))
        }
      }

      override def toggleSelection(item: ChallengeStatus): Unit = {
        updater { old =>
          val newStatus =
            if old.criteria.selectedStatus.exists(_.value == item.value) then None
            else allItems.find(_.value == item.value)
          old.focus(_.criteria.selectedStatus).replace(newStatus)
        }
      }

      override def removeSelectedItems(items: List[ChallengeStatus]): Unit = {
        updater { old =>
          if old.criteria.selectedStatus.exists(_.value == items.head.value) then
            old.focus(_.criteria.selectedStatus).replace(None)
          else old
        }
      }
    }

    val myTagProvider = new SingleTagGroupProvider {
      override def getAllItems: List[Tag] =
        val domain = getter().criteria.selectedDomain
        domain.subDomains.map(subdomain => Tag(subdomain.name, subdomain.slug, domain.slug))

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: Tag): Boolean =
        val state = getter().criteria
        if state.selectedDomain.slug == item.groupValue then state.selectedSubdomains.exists(_.slug == item.value)
        else false

      override def getSelectedItems: List[Tag] = {
        val state = getter().criteria
        state.selectedSubdomains.map { subdomain =>
          val domain = state.selectedDomain.slug
          Tag(subdomain.name, subdomain.slug, domain)
        }
      }

      override def addSelectedItems(items: List[Tag]): Unit = {
        updater { old =>
          old
            .focus(_.criteria.selectedSubdomains)
            .modify(_ ++ items.collect {
              case Tag(_, value, groupValue, _) if old.criteria.selectedDomain.slug == groupValue =>
                old.criteria.selectedDomain.subDomains.find(_.slug == value)
            }.collect { case Some(v) => v }.distinct)
        }
      }

      override def removeSelectedItems(items: List[Tag]): Unit = {
        updater { old =>
          old
            .focus(_.criteria.selectedSubdomains)
            .modify(_.filterNot(subdomain => items.exists(_.value == subdomain.slug)))
        }
      }

      override def toggleSelection(item: Tag): Unit =
        if isSelected(item) then removeSelectedItems(List(item))
        else addSelectedItems(List(item))
    }

    val mySkillProvider = new SkillParameterProvider {
      private val allItems: List[HackerRankChallengeSkill] =
        List(HackerRankChallengeSkill.Basic, HackerRankChallengeSkill.Intermediate, HackerRankChallengeSkill.Advanced)

      override def getAllItems: List[HackerRankChallengeSkill] = allItems

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: HackerRankChallengeSkill): Boolean =
        getter().criteria.selectedSkills.exists(_.value == item.value)

      override def getSelectedItems: List[HackerRankChallengeSkill] =
        getter().criteria.selectedSkills

      override def addSelectedItems(items: List[HackerRankChallengeSkill]): Unit = {
        updater { old => old.focus(_.criteria.selectedSkills).modify(skills => (skills ++ items).distinct) }
      }

      override def toggleSelection(item: HackerRankChallengeSkill): Unit = {
        updater { old =>
          val value =
            if old.criteria.selectedSkills.exists(_.value == item.value) then
              old.criteria.selectedSkills.filterNot(_.value == item.value)
            else old.criteria.selectedSkills :+ item
          old.focus(_.criteria.selectedSkills).replace(value)
        }
      }

      override def removeSelectedItems(items: List[HackerRankChallengeSkill]): Unit = {
        updater { old =>
          old.focus(_.criteria.selectedSkills).replace(old.criteria.selectedSkills.filterNot(items.contains))
        }
      }

    }

    dataSink.set(OpenChallengeActionGroup.CHALLENGE_PROVIDER_KEY, myChallengeProvider)
    dataSink.set(CategoryParameterAction.CATEGORY_PROVIDER_KEY, myCategoryProvider)
    dataSink.set(DifficultyParameterAction.DIFFICULTIES_PROVIDER_KEY, myDifficultiesProvider)
    dataSink.set(StatusParameterAction.STATUS_PROVIDER_KEY, myStatusProvider)
    dataSink.set(TagsAction.TAG_PROVIDER_KEY, myTagProvider)
    dataSink.set(SkillParameterAction.SKILL_PROVIDER_KEY, mySkillProvider)

    ActionManager.getInstance().getAction(Actions.HACKERRANK_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  }

  override protected def executeQuery(
    context:QueryContext[QueryParams] 
  ): IO[(Pagination, List[HackerRankChallengeDetail])] = {
    val from  = math.max((context.pagination.currentPage - 1) * context.pagination.pageSize.value, 0)
    val limit = context.pagination.pageSize.value
    val state = context.criteria
    HackerRankApi[IO]()
      .searchChallenges(
        from,
        limit,
        state.selectedDomain.contest,
        state.selectedDomain.slug,
        state.selectedStatus.toList,
        state.selectedSkills,
        state.selectedDifficulties,
        state.selectedSubdomains
      )
      .map { case (totalSize, items) =>
        (context.pagination.copy(totalSize = totalSize), items)
      }
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

  override protected def createQueryParametersTags(
    context: QueryContext[QueryParams],
    onCloseUpdater: (QueryContext[QueryParams] => QueryContext[QueryParams]) => Unit
  ): List[TagPaneAction] = {
    val myState = context.criteria
    List(
      TagPaneAction(myState.selectedDomain.slug, myState.selectedDomain.name, None, DOMAIN_TAG_RADIUS, None, None)
    ) ++
      myState.selectedDifficulties.map { difficulty =>
        TagPaneAction(
          difficulty.value,
          difficulty.showAsHtml,
          None,
          DIFFICULTY_TAG_RADIUS,
          None,
          Some(() =>
            onCloseUpdater(old => old.focus(_.criteria.selectedDifficulties).modify(_.filterNot(_ == difficulty)))
          )
        )
      } ++
      myState.selectedStatus.map { status =>
        TagPaneAction(
          status.value,
          status.show,
          None,
          STATUS_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(old => old.focus(_.criteria.selectedStatus).modify(_.filterNot(_ == status))))
        )
      } ++
      myState.selectedSkills.map { skill =>
        TagPaneAction(
          skill.value,
          skill.show,
          None,
          SKILL_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(old => old.focus(_.criteria.selectedSkills).modify(_.filterNot(_ == skill))))
        )
      } ++
      myState.selectedSubdomains.map { subdomain =>
        TagPaneAction(
          subdomain.slug,
          subdomain.name,
          None,
          SUBDOMAIN_TAG_RADIUS,
          None,
          Some(() =>
            onCloseUpdater(old =>
              old.focus(_.criteria.selectedSubdomains).modify(_.filterNot(it => it.slug == subdomain.slug))
            )
          )
        )
      }
  }
}

object HackerRankQueryParametersPresenter {

  case class QueryParams(
    selectedDomain: HackerRankChallengeDomain,
    selectedSubdomains: List[HackerRankChallengeSubdomain],
    selectedDifficulties: List[ChallengeDifficulty],
    selectedStatus: Option[ChallengeStatus],
    selectedSkills: List[HackerRankChallengeSkill]
  )

  private val DOMAIN_TAG_RADIUS     = 0.2f
  private val DIFFICULTY_TAG_RADIUS = 0.4f
  private val STATUS_TAG_RADIUS     = 0.5f
  private val SKILL_TAG_RADIUS      = 0.6f
  private val SUBDOMAIN_TAG_RADIUS  = 1.0f
}
