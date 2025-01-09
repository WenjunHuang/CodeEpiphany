package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import cats.effect.IO
import cats.effect.kernel.Resource.ExitCase
import cats.effect.std.Queue
import cats.syntax.all.*
import com.intellij.openapi.actionSystem.{ DataSink, UiDataProvider }
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.wenjunhuang.codeepiphany.actions.CodeDojoParameterAction.{ CODEDOJO_PROVIDER_KEY, CodeDojoParameterProvider }
import com.wenjunhuang.codeepiphany.actions.LanguageParameterAction.{ LANGUAGE_PROVIDER_KEY, LanguageParameterProvider }
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.*
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.jooq.{ Record, SelectOnConditionStep }
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import javax.swing.JComponent
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.Try

class SubmissionLogPresenter(private val myProject: Project) extends UiDataProvider with Disposable {
  private val myView        = SubmissionLogView(this)
  private val myLogger      = LoggerFactory[IO].getLogger
  private var myQueryParams = EMPTY_QUERY_PARAMS

  @volatile
  private var myQueryQueue: Option[Queue[IO, Option[QueryParams]]] = None

  private val myQueryWorker = for {
    q              <- Queue.unbounded[IO, Option[QueryParams]]
    _              <- IO.delay { myQueryQueue = Some(q) }
    notInterrupted <- SignallingRef.of[IO, Boolean](false)
    _ <- Stream
      .fromQueueNoneTerminated(q)
      // when a new query is received, we need to cancel the ongoing query to dump the old results
      .evalMapAccumulate(notInterrupted) { case (signal, state) =>
        for {
          _         <- signal.set(true)
          newSignal <- SignallingRef.of[IO, Boolean](false)
        } yield (newSignal, state)
      }
      .debounce(200.millis)
      .evalTap { _ =>
        IO.delay { refreshTags() }.evalOnEDTDefault()
      }
      .evalTap { case (signal, queryParams) =>
        Stream
          .eval(querySubmissionLogs(queryParams))
          .interruptWhen(signal)
          .attempt
          .onFinalizeCase {
            case ExitCase.Succeeded =>
              myLogger.info("Querying challenges is completed")
            case ExitCase.Canceled =>
              myLogger.info("Querying challenges is canceled")
            case ExitCase.Errored(e) =>
              myLogger.warn(e)("Error while querying challenges")
          }
          .compile
          .drain
          .evalAsBackgroundProgress(myProject, "Querying submission logs")
      }
      .onFinalize(myLogger.info("Query worker is finalized"))
      .compile
      .drain
  } yield ()

  myQueryWorker.unsafeRunAndForget()

  private val myDojoProvider = new CodeDojoParameterProvider {
    override def getAllItems: List[CodeDojo] = CodeDojo.values.toList

    override def isMultipleSelection: Boolean = true

    override def isSelected(item: CodeDojo): Boolean = myQueryParams.dojos.contains(item)

    override def getSelectedItems: List[CodeDojo] = myQueryParams.dojos

    override def addSelectedItems(items: List[CodeDojo]): Unit = {
      myQueryParams = myQueryParams.copy(dojos = (myQueryParams.dojos ++ items).distinct)
      requery()
    }

    override def toggleSelection(item: CodeDojo): Unit = {
      if myQueryParams.dojos.contains(item) then
        myQueryParams = myQueryParams.copy(dojos = myQueryParams.dojos.filterNot(_ == item))
      else myQueryParams = myQueryParams.copy(dojos = myQueryParams.dojos :+ item)
      requery()
    }

    override def removeSelectedItems(items: List[CodeDojo]): Unit = {
      myQueryParams = myQueryParams.copy(dojos = myQueryParams.dojos.filterNot(items.contains))
      requery()
    }
  }

  private val myLanguageProvider = new LanguageParameterProvider {
    override def getAllItems: List[(Language, LanguageVersion)] = {
      val repository = ChallengeRepository.getInstance(myProject)
      val dsl        = repository.getDSLContext
      dsl
        .selectDistinct(CHALLENGE_LANGUAGE.LANGUAGE, CHALLENGE_LANGUAGE.LANGUAGEVERSION)
        .from(CHALLENGE_LANGUAGE)
        .orderBy(CHALLENGE_LANGUAGE.LANGUAGE.asc(), CHALLENGE_LANGUAGE.LANGUAGEVERSION.asc())
        .fetch()
        .asScala
        .map { record =>
          (
            Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE))).get,
            LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
          )
        }
        .toList
    }

    override def isMultipleSelection: Boolean = true

    override def isSelected(item: (Language, LanguageVersion)): Boolean =
      myQueryParams.languages.contains(item)

    override def getSelectedItems: List[(Language, LanguageVersion)] = myQueryParams.languages

    override def addSelectedItems(items: List[(Language, LanguageVersion)]): Unit = {
      myQueryParams = myQueryParams.copy(languages = (myQueryParams.languages ++ items).distinct)
      requery()
    }

    override def toggleSelection(item: (Language, LanguageVersion)): Unit = {
      if myQueryParams.languages.contains(item) then
        myQueryParams = myQueryParams.copy(languages = myQueryParams.languages.filterNot(_ == item))
      else myQueryParams = myQueryParams.copy(languages = myQueryParams.languages :+ item)
      requery()
    }

    override def removeSelectedItems(items: List[(Language, LanguageVersion)]): Unit = {
      myQueryParams = myQueryParams.copy(languages = myQueryParams.languages.filterNot(items.contains))
      requery()
    }
  }

  def getView: JComponent = myView

  private def requery(): Unit = {
    myQueryQueue.foreach(_.offer(Some(myQueryParams)).unsafeRunAndForget())
  }

  private def querySubmissionLogs(queryParams: QueryParams): IO[List[SubmissionLogEntry]] = {
    val repository = ChallengeRepository.getInstance(myProject)
    repository.getDSLContextResource[IO].use { dsl =>
      IO.delay {
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

        queryParams
          .fillQueryConditions(base)
          .fetch()
          .asScala
          .map { record =>
            Try {
              SubmissionLogEntry(
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
          .filter(_.isSuccess)
          .map(_.get)
          .toList
      }
    }
  }

  private def refreshTags(): Unit = {
    val tagPane = myView.getTagPane
    tagPane.removeAllTags()

    myQueryParams.dojos.foreach { dojo =>
      tagPane.addTagAction(
        dojo.value,
        dojo.show,
        dojo.getIcon,
        CODEDOJO_TAG_RADIUS,
        Some(() => myDojoProvider.removeSelectedItems(List(dojo)))
      )
    }
    myQueryParams.languages.foreach { case (language, version) =>
      tagPane.addTagAction(
        s"${language.show} ${version.version}",
        s"${language.show} ${version.version}",
        Option(language.icon),
        LANGUAGE_TAG_RADIUS,
        Some(() => myLanguageProvider.removeSelectedItems(List(language -> version)))
      )
    }

    myView.getTagPane.updateActionsAsync()
  }

  override def dispose(): Unit = {
    myQueryQueue.foreach(_.offer(None).unsafeRunSync())
  }

  override def uiDataSnapshot(dataSink: DataSink): Unit = {
    dataSink.set(CODEDOJO_PROVIDER_KEY, myDojoProvider)
    dataSink.set(LANGUAGE_PROVIDER_KEY, myLanguageProvider)
  }
}

object SubmissionLogPresenter {
  private val EMPTY_QUERY_PARAMS = QueryParams(
    dojos = List.empty,
    dojoOrder = None,
    languages = List.empty,
    languageOrder = None,
    difficulties = List.empty,
    difficultiesOrder = None,
    submitResults = List.empty,
    submitResultsOrder = None,
    submissionDateTimeOrder = None,
    resultDateTimeOrder = None,
    titleKeyword = None
  )
  private case class QueryParams(
                                  dojos: List[CodeDojo],
                                  dojoOrder: Option[OrderFilter],
                                  languages: List[(Language, LanguageVersion)],
                                  languageOrder: Option[OrderFilter],
                                  difficulties: List[ChallengeDifficulty],
                                  difficultiesOrder: Option[OrderFilter],
                                  submitResults: List[SubmissionResult],
                                  submitResultsOrder: Option[OrderFilter],
                                  submissionDateTimeOrder: Option[OrderFilter],
                                  resultDateTimeOrder: Option[OrderFilter],
                                  titleKeyword: Option[String]
  ) {
    def fillQueryConditions(base: SelectOnConditionStep[Record]) = {
      var query = base
      if dojos.nonEmpty then query = query.and(CHALLENGE.DOJO.in(dojos.map(_.value).asJava))
      if languages.nonEmpty then
        query = query.and(
          CHALLENGE_LANGUAGE.LANGUAGE
            .in(languages.map(_._1.value).asJava)
            .and(CHALLENGE_LANGUAGE.LANGUAGEVERSION.in(languages.map(_._2.toString).asJava))
        )
      if difficulties.nonEmpty then query = query.and(CHALLENGE.DIFFICULTY.in(difficulties.map(_.value).asJava))
      if submitResults.nonEmpty then query = query.and(SOLUTION_SUBMISSION.RESULT.in(submitResults.map(_.value).asJava))
      if titleKeyword.nonEmpty then query = query.and(CHALLENGE.TITLE.likeIgnoreCase(s"%${titleKeyword.get}%"))

      List(
        languageOrder.map(_.toJooqSortField(CHALLENGE_LANGUAGE.LANGUAGE)),
        dojoOrder.map(_.toJooqSortField(CHALLENGE.DOJO)),
        difficultiesOrder.map(_.toJooqSortField(CHALLENGE.DIFFICULTY)),
        submitResultsOrder.map(_.toJooqSortField(SOLUTION_SUBMISSION.RESULT)),
        submissionDateTimeOrder.map(_.toJooqSortField(SOLUTION_SUBMISSION.SUBMITDATETIME)),
        resultDateTimeOrder.map(_.toJooqSortField(SOLUTION_SUBMISSION.RESULTDATETIME))
      ).filter(_.nonEmpty)
        .map(_.get) match
        case Nil => query
        case orders =>
          query.orderBy(orders.toArray*)
    }
  }
  private val LANGUAGE_TAG_RADIUS = 0.3f
  private val CODEDOJO_TAG_RADIUS = 0.5f
}
