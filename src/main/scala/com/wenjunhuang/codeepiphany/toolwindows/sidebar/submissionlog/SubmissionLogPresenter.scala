package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import cats.effect.IO
import cats.syntax.all.*
import java.time.format.DateTimeFormatter
import javax.swing.{DefaultListSelectionModel, Icon, JTable, ListSelectionModel}
import javax.swing.table.TableCellRenderer
import monocle.syntax.all.*
import org.jooq.SelectOnConditionStep
import org.typelevel.ci.CIString
import scala.jdk.CollectionConverters.*
import scala.util.{Success, Try}

import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.actions.CompareFilesAction
import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager}
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.table.IconTableCellRenderer

import com.wenjunhuang.codeepiphany.actions.CodeDojoParameterAction.{CODEDOJO_PROVIDER_KEY, CodeDojoParameterProvider}
import com.wenjunhuang.codeepiphany.actions.LanguageParameterAction.{LANGUAGE_PROVIDER_KEY, LanguageParameterProvider}
import com.wenjunhuang.codeepiphany.actions.OpenSubmissionCodeAction.{OPEN_SUBMISSION_PROVIDER_KEY, OpenSubmissionCodeProvider}
import com.wenjunhuang.codeepiphany.database.tables.records.{HackerrankSubmissionCaseRecord, LeetcodeSubmissionRecord, SolutionSubmissionRecord}
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.{ChallengeRepository, ParametersQueryPresenter, QueryContext}
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.*
import com.wenjunhuang.codeepiphany.utils.{OrderByColumnInfo, Pagination}
import com.wenjunhuang.codeepiphany.utils.ui.TagPaneAction
import com.wenjunhuang.codeepiphany.vfs.SubmissionCodeFileSystem
import com.wenjunhuang.codeepiphany.vfs.SubmissionCodeFileSystem.SubmissionCodeFilePath
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.utils.actions.DataSink

class SubmissionLogPresenter(project: Project)
    extends ParametersQueryPresenter[Unit, QueryParams, SubmissionLogEntry](project, ()) {

  override def getRowSelectionTablePopup: ActionGroup = {
    ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TABLE_POPUP_GROUP).asInstanceOf[ActionGroup]
  }

  override protected def prepareProviders(
    getter: () => QueryContext[QueryParams],
    updater: (QueryContext[QueryParams] => QueryContext[QueryParams]) => Unit,
    dataSink: DataSink
  ): ActionGroup = {

    val myCodeDojoProvider = new CodeDojoParameterProvider {
      override def getAllItems: List[CodeDojo] = CodeDojo.values.toList

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: CodeDojo): Boolean = getter().criteria.dojos.contains(item)

      override def getSelectedItems: List[CodeDojo] = getter().criteria.dojos

      override def addSelectedItems(items: List[CodeDojo]): Unit = {
        updater { old =>
          old.focus(_.criteria.dojos).modify { dojos =>
            (dojos ++ items).distinct
          }
        }
      }

      override def toggleSelection(item: CodeDojo): Unit = {
        updater { old =>
          old.focus(_.criteria.dojos).modify { dojos =>
            if dojos.contains(item) then dojos.filterNot(_ == item)
            else dojos :+ item
          }
        }
      }

      override def removeSelectedItems(items: List[CodeDojo]): Unit = {
        updater { old =>
          old.focus(_.criteria.dojos).modify { dojos =>
            dojos.filterNot(items.contains)
          }
        }
      }
    }

    val myLanguageProvider = new LanguageParameterProvider {
      override def getAllItems: List[Language] = {
        ChallengeRepository
          .getInstance(myProject)
          .getDSLContext
          .selectDistinct(CHALLENGE_LANGUAGE.LANGUAGE)
          .from(CHALLENGE_LANGUAGE)
          .orderBy(CHALLENGE_LANGUAGE.LANGUAGE.asc())
          .fetch()
          .asScala
          .map { record => Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE))) }
          .collect { case Some(l) => l }
          .toList
      }

      override def isMultipleSelection: Boolean = true

      override def isSelected(item: Language): Boolean =
        getter().criteria.languages.contains(item)

      override def getSelectedItems: List[Language] = getter().criteria.languages

      override def addSelectedItems(items: List[Language]): Unit = {
        updater { old =>
          old.focus(_.criteria.languages).modify { languages =>
            (languages ++ items).distinct
          }
        }
      }

      override def toggleSelection(item: Language): Unit = {
        updater { old =>
          old.focus(_.criteria.languages).modify { languages =>
            if languages.contains(item) then languages.filterNot(_ == item)
            else languages :+ item
          }
        }
      }

      override def removeSelectedItems(items: List[Language]): Unit = {
        updater { old =>
          old.focus(_.criteria.languages).modify { languages =>
            languages.filterNot(items.contains)
          }
        }
      }
    }

    val myOpenSubmissionCodeProvider = new OpenSubmissionCodeProvider {
      override def openSubmissionCode(): Unit = {
        myQueryResultSelectionModel.getSelectedIndices.headOption match {
          case Some(index) =>
            val id   = myQueryResultTableModel.getItem(index).id
            val file = getSubmissionCodeFile(id)
            if file != null then FileEditorManager.getInstance(myProject).openFile(file)
          case None =>
        }
      }
    }

    dataSink.set(CODEDOJO_PROVIDER_KEY, myCodeDojoProvider)
    dataSink.set(LANGUAGE_PROVIDER_KEY, myLanguageProvider)
    dataSink.set(OPEN_SUBMISSION_PROVIDER_KEY, myOpenSubmissionCodeProvider)
    dataSink.`lazy`(
      CompareFilesAction.DIFF_REQUEST,
      { () =>
        myQueryResultSelectionModel.getSelectedIndices.toList match
          case f :: s :: Nil =>
            val first  = myQueryResultTableModel.getItem(f)
            val second = myQueryResultTableModel.getItem(s)
            SimpleDiffRequest(
              "Submission Log Diff",
              DiffContentFactory.getInstance().create(myProject, getSubmissionCodeFile(first.id)),
              DiffContentFactory.getInstance().create(myProject, getSubmissionCodeFile(second.id)),
              createDiffTitle(first),
              createDiffTitle(second)
            )
          case _ => null
      }
    )
    ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  }

  private def createDiffTitle(logEntry: SubmissionLogEntry): String = {
    s"${logEntry.solution}-${logEntry.challengeTitle}-${logEntry.submissionDateTime.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}"
  }

  private def getSubmissionCodeFile(submissionId: Long): VirtualFile = {
    SubmissionCodeFileSystem
      .getInstance()
      .findOrCreateFile(
        myProject,
        SubmissionCodeFilePath(submissionId, myProject, SubmissionCodeFileSystem.CodeType.Submission)
      )
  }
  override protected def createQueryResultSelectionModel(): ListSelectionModel =
    DefaultListSelectionModel()

  override protected def createQueryParametersTags(
    context: QueryContext[QueryParams],
    onCloseUpdater: (QueryContext[QueryParams] => QueryContext[QueryParams]) => Unit
  ): List[TagPaneAction] = {
    context.criteria.dojos.map { dojo =>
      TagPaneAction(
        dojo.value,
        dojo.show,
        dojo.getIcon,
        CODEDOJO_TAG_RADIUS,
        None,
        Some(() => onCloseUpdater(_.focus(_.criteria.dojos).modify(_ filterNot (_ == dojo))))
      )
    } ++
      context.criteria.languages.map { language =>
        TagPaneAction(
          s"${language.show}",
          s"${language.show}",
          Option(language.icon),
          LANGUAGE_TAG_RADIUS,
          None,
          Some(() => onCloseUpdater(_.focus(_.criteria.languages).modify(_ filterNot (_ == language))))
        )
      }
  }

  override protected def createInitialQueryParameters(boostrapParameters: Unit): QueryContext[QueryParams] = {
    QueryContext[QueryParams](QueryParams(dojos = List.empty, languages = List.empty, orderBy = None), Pagination())
  }

  override protected def executeQuery(
    context: QueryContext[QueryParams]
  ): IO[(Pagination, List[SubmissionLogEntry])] = {
    ChallengeRepository.getInstance(myProject).getDSLContextResource[IO].use { dsl =>
      IO.delay {
        val total = context.criteria
          .fillQueryConditions(
            dsl
              .selectCount()
              .from(SOLUTION_SUBMISSION)
              .innerJoin(SOLUTION)
              .on(SOLUTION_SUBMISSION.SOLUTIONID.eq(SOLUTION.ID))
              .innerJoin(CHALLENGE_LANGUAGE)
              .on(SOLUTION_SUBMISSION.CHALLENGELANGUAGEID.eq(CHALLENGE_LANGUAGE.ID))
              .innerJoin(CHALLENGE)
              .on(CHALLENGE_LANGUAGE.CHALLENGEID.eq(CHALLENGE.ID))
          )
          .fetchOne(0, classOf[Int])

        val base = dsl
          .select(
            (SOLUTION_SUBMISSION.fields() ++
              SOLUTION.fields() ++
              CHALLENGE_LANGUAGE.fields() ++
              CHALLENGE.fields())*
          )
          .from(SOLUTION_SUBMISSION)
          .innerJoin(SOLUTION)
          .on(SOLUTION_SUBMISSION.SOLUTIONID.eq(SOLUTION.ID))
          .innerJoin(CHALLENGE_LANGUAGE)
          .on(SOLUTION_SUBMISSION.CHALLENGELANGUAGEID.eq(CHALLENGE_LANGUAGE.ID))
          .innerJoin(CHALLENGE)
          .on(CHALLENGE_LANGUAGE.CHALLENGEID.eq(CHALLENGE.ID))

        val items = context.criteria
          .fillQueryConditions(base)
          .limit(
            (context.pagination.currentPage - 1) * context.pagination.pageSize.value,
            context.pagination.pageSize.value
          )
          .fetch()
          .asScala
          .map { record =>
            Try {
              SubmissionLogEntry(
                id = record.get(SOLUTION_SUBMISSION.ID).toLong,
                dojo = CodeDojo.fromCIString(CIString(record.get(CHALLENGE.DOJO))).get,
                challengeTitle = record.get(CHALLENGE.TITLE),
                solution = record.get(SOLUTION.TITLE),
                language = Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE))).get,
                languageVersion = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION)),
                difficulty = ChallengeDifficulty.fromCIString(CIString(record.get(CHALLENGE.DIFFICULTY))).get,
                resultMessage = record.get(SOLUTION_SUBMISSION.MESSAGE),
                result = SubmissionResult.fromCIString(CIString(record.get(SOLUTION_SUBMISSION.RESULT))).get,
                submissionDateTime = Option(record.get(SOLUTION_SUBMISSION.SUBMITDATETIME)),
                resultDateTime = Option(record.get(SOLUTION_SUBMISSION.RESULTDATETIME))
              )
            }
          }
          .collect { case Success(r) =>
            r
          }
          .toList
        (context.pagination.copy(totalSize = total), items)
      }
    }
  }

  private def getDirectionOf(field: SubmissionLogOrderBy): Option[OrderDirection] =
    myQueryStateManager.get.criteria.orderBy.collect {
      case (f, d) if f == field => d
    }

  private def setDirectionOf(field: SubmissionLogOrderBy, direction: Option[OrderDirection]): Unit = {
    myQueryStateManager.update { old =>
      direction match
        case None => old.focus(_.criteria.orderBy).replace(None)
        case Some(direction) =>
          old.focus(_.criteria.orderBy).replace(Some((field, direction)))
    }
    requery(true)
  }

  override def getQueryResultColumns: Array[OrderByColumnInfo[SubmissionLogEntry, ?]] = Array(
    new OrderByColumnInfo[SubmissionLogEntry, CodeDojo](PluginBundle.message("submissionLog.ui.dojo.title")) {
      override def valueOf(item: SubmissionLogEntry): CodeDojo = item.dojo

      override def getPreferredStringValue: String = PluginBundle.message("submissionLog.ui.dojo.title")

      override def getRenderer(item: SubmissionLogEntry): TableCellRenderer =
        new IconTableCellRenderer[CodeDojo]() {
          setToolTipText(item.dojo.show)
          override def getIcon(value: CodeDojo, table: JTable, row: Int): Icon =
            setText(value.show)
            value.getIcon.orNull

          override def isCenterAlignment: Boolean = true
        }

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] = getDirectionOf(SubmissionLogOrderBy.CodeDojoField)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(SubmissionLogOrderBy.CodeDojoField, filter)
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.challenge.title")) {
      override def valueOf(item: SubmissionLogEntry): String = item.challengeTitle

      override def getPreferredStringValue: String = StringUtil.repeat("W", 30)

    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.solution.title")) {
      override def valueOf(item: SubmissionLogEntry): String = item.solution
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.difficulty.title")) {
      override def valueOf(item: SubmissionLogEntry): String = s"${item.difficulty.showAsHtml}"
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(SubmissionLogOrderBy.DifficultyField)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(SubmissionLogOrderBy.DifficultyField, filter)
    },
    new OrderByColumnInfo[SubmissionLogEntry, (Language, LanguageVersion)](
      PluginBundle.message("submissionLog.ui.language.title")
    ) {
      override def valueOf(item: SubmissionLogEntry): (Language, LanguageVersion) =
        (item.language, item.languageVersion)
      override def getPreferredStringValue: String = StringUtil.repeat("W", 15)

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(SubmissionLogOrderBy.LanguageField)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(SubmissionLogOrderBy.LanguageField, filter)

      override def getRenderer(item: SubmissionLogEntry): TableCellRenderer =
        new IconTableCellRenderer[(Language, LanguageVersion)]() {
          override def getIcon(value: (Language, LanguageVersion), table: JTable, row: Int): Icon =
            setText(Language.prettyPrint(value._1, value._2))
            value._1.icon
        }
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](PluginBundle.message("submissionLog.ui.result.title")) {
      override def valueOf(item: SubmissionLogEntry): String = s"${item.result.showAsHtml}"
      override def getPreferredStringValue: String           = StringUtil.repeat("W", 15)

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(SubmissionLogOrderBy.SubmissionResultField)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(SubmissionLogOrderBy.SubmissionResultField, filter)
    },
    new OrderByColumnInfo[SubmissionLogEntry, String](
      PluginBundle.message("submissionLog.ui.submissionDateTime.title")
    ) {
      override def valueOf(item: SubmissionLogEntry): String =
        s"${item.submissionDateTime.map(_.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).getOrElse("")}"
      override def getPreferredStringValue: String = StringUtil.repeat("W", 20)

      override def enableOrderBy: Boolean = true

      override def getOrderFilter: Option[OrderDirection] =
        getDirectionOf(SubmissionLogOrderBy.SubmissionDateTimeField)

      override def setOrderFilter(filter: Option[OrderDirection]): Unit =
        setDirectionOf(SubmissionLogOrderBy.SubmissionDateTimeField, filter)
    }
  )
}

object SubmissionLogPresenter {
  enum SubmissionLogOrderBy {
    case CodeDojoField
    case LanguageField
    case DifficultyField
    case SubmissionResultField
    case SubmissionDateTimeField
  }

  enum SubmissionType {
    case LeetCodeSubmission(
      language: Language,
      languageVersion: LanguageVersion,
      challengeSlug: String,
      record: SolutionSubmissionRecord,
      leetCodeSubmission: LeetcodeSubmissionRecord
    )
    case LeetCodeCNSubmission(
      language: Language,
      languageVersion: LanguageVersion,
      challengeSlug: String,
      record: SolutionSubmissionRecord,
      leetCodeSubmission: LeetcodeSubmissionRecord
    )
    case HackerRankSubmission(
      language: Language,
      languageVersion: LanguageVersion,
      challengeSlug: String,
      contestSlug: Option[String],
      record: SolutionSubmissionRecord,
      hackerCases: List[HackerrankSubmissionCaseRecord]
    )
    case CodeForcesSubmission(
      language: Language,
      languageVersion: LanguageVersion,
      record: SolutionSubmissionRecord,
      contestId: Long,
      problemsetName: Option[String]
    )
    case AtCoderSubmission(
      language: Language,
      languageVersion: LanguageVersion,
      record: SolutionSubmissionRecord,
      contestId: String,
      problemId: String
    )
    case LuoGuSubmission(language: Language, languageVersion: LanguageVersion, record: SolutionSubmissionRecord)
  }

  private val EMPTY_QUERY_PARAMS = QueryParams(dojos = List.empty, languages = List.empty, orderBy = None)

  case class QueryParams(
    dojos: List[CodeDojo],
    languages: List[Language],
    orderBy: Option[(SubmissionLogOrderBy, OrderDirection)]
  ) {
    def fillQueryConditions(base: SelectOnConditionStep[?]) = {
      var query = base
      if dojos.nonEmpty then query = query.and(CHALLENGE.DOJO.in(dojos.map(_.value).asJava))
      if languages.nonEmpty then query = query.and(CHALLENGE_LANGUAGE.LANGUAGE.in(languages.map(_.value).asJava))

      orderBy match
        case None => query
        case Some((field, direction)) =>
          field match
            case SubmissionLogOrderBy.CodeDojoField => query.orderBy(direction.toJooqSortField(CHALLENGE.DOJO))
            case SubmissionLogOrderBy.LanguageField =>
              query.orderBy(direction.toJooqSortField(CHALLENGE_LANGUAGE.LANGUAGE))
            case SubmissionLogOrderBy.DifficultyField => query.orderBy(direction.toJooqSortField(CHALLENGE.DIFFICULTY))
            case SubmissionLogOrderBy.SubmissionResultField =>
              query.orderBy(direction.toJooqSortField(SOLUTION_SUBMISSION.RESULT))
            case SubmissionLogOrderBy.SubmissionDateTimeField =>
              query.orderBy(direction.toJooqSortField(SOLUTION_SUBMISSION.SUBMITDATETIME))
    }
  }

  private val LANGUAGE_TAG_RADIUS = 0.3f
  private val CODEDOJO_TAG_RADIUS = 0.5f
}
