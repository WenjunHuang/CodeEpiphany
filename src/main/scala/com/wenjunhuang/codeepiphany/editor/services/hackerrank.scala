package com.wenjunhuang.codeepiphany.editor.services

import cats.effect.{Async, Concurrent}
import cats.effect.kernel.Resource.ExitCase
import cats.syntax.all.*
import fs2.Stream
import java.time.LocalDateTime
import org.jooq.impl.DSL
import org.jooq.{DSLContext, Record}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.jdk.OptionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{VirtualFile, VirtualFileUtil}

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.hackerrank.model.{HackerRankContest, HackerRankRunCodeResponse, HackerRankSubmissionResponse}
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.SubmissionResult.CompilationError
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.IdGenerator

class HackerRankService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory] {

  private case class HackerRankChallengeInfo(
    contest: HackerRankContest,
    language: Language,
    languageVersion: LanguageVersion,
    slug: String
  )

  def runCode(vf: VirtualFile, project: Project, item: ChallengeSettingsStateItem): F[Unit] = {
    item.dojo match {
      case CodeDojo.HackerRank =>
        for {
          challengeInfo <- fetchChallengeInfo(project, item)
          localCode     <- readLocalCode(vf, challengeInfo.language)
          processedCode <- extractCode(localCode, challengeInfo.language)
          _ <- executeHackerRankRun(challengeInfo, processedCode)
            .evalMap(response => handleRunResult(project, response))
            .compile
            .drain
        } yield ()
      case _ =>
        Async[F].raiseError(new IllegalArgumentException("Unsupported HackerRank platform"))
    }
  }

  def submitCode(vf: VirtualFile, project: Project, item: ChallengeSettingsStateItem): F[Unit] = item.dojo match {
    case CodeDojo.HackerRank =>
      for {
        challengeInfo <- fetchChallengeInfo(project, item)
        localCode     <- readLocalCode(vf, challengeInfo.language)
        processedCode <- extractCode(localCode, challengeInfo.language)
        submissionId  <- storeSubmission(project, item, localCode, processedCode)
        _             <- executeHackerRankSubmit(project, challengeInfo, processedCode, submissionId)
      } yield ()
    case _ =>
      Async[F].raiseError(new IllegalArgumentException("Unsupported HackerRank platform"))
  }

  private def executeHackerRankSubmit(
    project: Project,
    info: HackerRankChallengeInfo,
    code: String,
    submissionId: Long
  ): F[Unit] = {
    HackerRankApi[F]()
      .submitAnswer(info.slug, info.contest, info.language, info.languageVersion.version, code)
      .evalMap(response => updateSubmissionRecord(project, submissionId, response).map(it => (it._1, it._2, response)))
      .compile
      .last
      .flatMap {
        case Some((result, message, response)) =>
          reportSubmitResult(project, result, message)
        case _ => Async[F].unit
      }
      .handleErrorWith(error => updateSubmissionOnError(project, submissionId, error) >> Async[F].raiseError(error))
  }

  private def reportSubmitResult(project: Project, result: SubmissionResult, message: String): F[Unit] = {
    result match
      case SubmissionResult.Success =>
        console.info[F](project, "🎉 Passed!")
      case SubmissionResult.Failure =>
        console.error[F](project, message)
      case SubmissionResult.RuntimeError =>
        console.error[F](project, message)
      case SubmissionResult.CompilationError =>
        console.error[F](project, s"Compilation Error: \n ${message}")
      case SubmissionResult.Processing =>
        console.info[F](project, message)
      case SubmissionResult.Timeout =>
        console.error[F](project, message)
      case SubmissionResult.Unknown =>
        console.error[F](project, message)
  }

  private def updateSubmissionOnError(project: Project, submissionId: Long, error: Throwable): F[Unit] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use { dsl =>
        Async[F].delay {
          dsl
            .update(SOLUTION_SUBMISSION)
            .set(SOLUTION_SUBMISSION.RESULT, SubmissionResult.Failure.value)
            .set(SOLUTION_SUBMISSION.MESSAGE, error.getMessage)
            .where(SOLUTION_SUBMISSION.ID.eq(submissionId))
            .execute()
        }
      }
  }

  private def updateSubmissionRecord(
    project: Project,
    submissionId: Long,
    response: HackerRankSubmissionResponse
  ): F[(SubmissionResult, String)] = {
    ChallengeRepository.getInstance(project).getDSLContextResource.use { client =>
      Async[F].blocking {
        client.transactionResult { trx =>
          val dsl = DSL.using(trx)
          dsl
            .selectFrom(SOLUTION_SUBMISSION)
            .where(SOLUTION_SUBMISSION.ID.eq(submissionId))
            .fetchOptional()
            .toScala
            .map { record =>
              val result = toSubmissionResult(response.status)
              val message =
                if result == SubmissionResult.CompilationError then response.compileMessage.getOrElse(response.status)
                else response.status
              record
                .setDojosubmissionid(response.id.toString)
                .setResultdatetime(LocalDateTime.now())
                .setResult(result.value)
                .setScore(response.score)
                .setMessage(message)
              record.store()

              response.codecheckerSignal
                .zip(response.codecheckerTime)
                .zip(response.testcaseMessage)
                .zip(response.testcaseStatus)
                .map { case (((a, b), c), d) => (a, b, c, d) }
                .zipWithIndex
                .foreach { (item, index) =>
                  val (signal, time, message, status) = item
                  val testcaseRecord = dsl
                    .newRecord(HACKERRANK_SUBMISSION_CASE)
                    .setId(IdGenerator.nextId())
                    .setSubmissionid(submissionId)
                    .setTestcasemessage(message)
                    .setNum(index)
                    .setTestcasestatus(status)
                    .setCodecheckersignal(signal)
                    .setCodecheckertime(time.bigDecimal.floatValue())
                  testcaseRecord.store()
                }
              (result, message)
            }
            .getOrElse(throw new Exception("Cannot find submission record"))
        }
      }
    }
  }

  private def toSubmissionResult(status: String): SubmissionResult = {
    val ci = CIString(status)
    if ci.contains(CIString("Accepted")) then SubmissionResult.Success
    else if ci.contains(CIString("Wrong Answer")) then SubmissionResult.Failure
    else if ci.contains(CIString("Compilation error")) then CompilationError
    else if ci.contains(CIString("Terminated due to timeout")) then SubmissionResult.Timeout
    else if ci.contains(CIString("Processing")) then SubmissionResult.Processing
    else SubmissionResult.Unknown
  }

  private def storeSubmission(
    project: Project,
    item: ChallengeSettingsStateItem,
    localCode: String,
    submitCode: String
  ): F[Long] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use { client =>
        Async[F].delay {
          client.transactionResult { trx =>
            val dsl = DSL.using(trx)
            val submissionRecord = dsl
              .newRecord(SOLUTION_SUBMISSION)
              .setId(IdGenerator.nextId())
              .setChallengelanguageid(item.challengeLanguageId)
              .setLocalcode(localCode)
              .setSubmitcode(submitCode)
              .setSubmitdatetime(java.time.LocalDateTime.now())
              .setSolutionid(item.solutionId)
              .setResult(SubmissionResult.Processing.value)
            submissionRecord.store()
            submissionRecord.getId
          }
        }
      }
  }

  private def readLocalCode(vf: VirtualFile, language: Language): F[String] =
    Async[F].blocking {
      VirtualFileUtil.readText(vf)
    }

  private def handleRunResult(project: Project, response: HackerRankRunCodeResponse): F[Unit] = {
    if response.status == 0 then console.info[F](project, "Running...")
    else
      response.compilemessage.filter(_.nonEmpty) match
        case Some(message) =>
          console.error[F](project, s"Compilation Error: \n ${message}")
        case None =>
          if response.testcaseStatus.contains(0) then console.error[F](project, "Wrong Answer!")
          else console.info[F](project, "🎉 Passed!")
  }

  private def extractCode(rawCode: String, language: Language): F[String] =
    Async[F].blocking {
      if CodeDojo.HackerRank.requiresCodeRegionEnclosure then language.extractCodeFromRegion(rawCode)
      else rawCode
    }

  private def fetchChallengeInfo(project: Project, item: ChallengeSettingsStateItem): F[HackerRankChallengeInfo] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use { client =>
        Async[F].delay(queryChallengeBasicInfo(item, client))
      }
  }

  private def queryChallengeBasicInfo(item: ChallengeSettingsStateItem, client: DSLContext): HackerRankChallengeInfo = {
    client
      .select(
        CHALLENGE.SLUG,
        HACKERRANK_CHALLENGE.CONTESTSLUG,
        CHALLENGE_LANGUAGE.LANGUAGE,
        CHALLENGE_LANGUAGE.LANGUAGEVERSION
      )
      .from(CHALLENGE)
      .innerJoin(HACKERRANK_CHALLENGE)
      .on(CHALLENGE.ID.eq(HACKERRANK_CHALLENGE.ID))
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
      .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
      .fetchOptional()
      .toScala
      .flatMap(parseHackerRankRecord)
      .getOrElse(throw new Exception("Cannot find data for file"))
  }

  private def parseHackerRankRecord(record: Record): Option[HackerRankChallengeInfo] = {
    for {
      challengeSlug <- Option(record.get(CHALLENGE.SLUG))
      contestSlug <- Option(record.get(HACKERRANK_CHALLENGE.CONTESTSLUG))
        .flatMap(it => HackerRankContest.fromCIString(CIString(it)))
      language <- Option(record.get(CHALLENGE_LANGUAGE.LANGUAGE))
        .flatMap(it => Language.fromCIString(CIString(it)))
      langVer <- Option(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
    } yield HackerRankChallengeInfo(contestSlug, language, LanguageVersion.fromString(langVer), challengeSlug)
  }

  private def executeHackerRankRun(
    info: HackerRankChallengeInfo,
    code: String
  ): Stream[F, HackerRankRunCodeResponse] = {
    HackerRankApi[F]()
      .runAnswer(info.slug, info.contest, info.language, info.languageVersion.version, code)
  }
}

object hackerrank {
  def runCode[F[_]: Async: Concurrent: HttpClientManager](
    vf: VirtualFile,
    project: Project,
    item: ChallengeSettingsStateItem
  ): F[Unit] = {
    Stream
      .eval(
        ChallengeRepository
          .getInstance(project)
          .getDSLContextResource[F]
          .use(dsl => Async[F].delay(queryChallengeBasicInfo(item, dsl)))
      )
      .flatMap { case (contest, language, langVer, challengeSlug) =>
        val extractedCode = language.extractCodeFromRegion(VirtualFileUtil.readText(vf))
        HackerRankApi[F]()
          .runAnswer(challengeSlug, contest, language, langVer, extractedCode)
      }
      .last
      .evalTap {
        case Some(response) =>
          response.compilemessage.filter(_.nonEmpty) match
            case Some(message) =>
              console.error[F](project, s"Compilation Error: \n ${message}")
            case None =>
              if response.testcaseStatus.contains(0) then console.error[F](project, "Wrong Answer!")
              else console.info[F](project, "🎉 Passed!")
        case None => Async[F].unit
      }
      .onFinalizeCase {
        case ExitCase.Succeeded =>
          Async[F].unit
        case ExitCase.Errored(e) =>
          console.error[F](project, s"Error to run code: \n ${e.getMessage}")
        case ExitCase.Canceled =>
          console.warn[F](project, s"Code submission cancelled for ${vf.getCanonicalPath}")
      }
      .compile
      .drain
  }

  def submitCode[F[_]: Async: Concurrent: HttpClientManager](
    vf: VirtualFile,
    project: Project,
    item: ChallengeSettingsStateItem
  ): F[Unit] = {
    Stream
      .eval(
        ChallengeRepository
          .getInstance(project)
          .getDSLContextResource[F]
          .use { client =>
            Async[F].delay {
              client.transactionResult { trx =>
                val dsl        = DSL.using(trx)
                val basicInfo  = queryChallengeBasicInfo(item, dsl)
                val localCode  = VirtualFileUtil.readText(vf)
                val submitCode = basicInfo._2.extractCodeFromRegion(localCode)
                val solutionId = item.solutionId

                val submissionRecord = dsl
                  .newRecord(SOLUTION_SUBMISSION)
                  .setId(IdGenerator.nextId())
                  .setChallengelanguageid(item.challengeLanguageId)
                  .setLocalcode(localCode)
                  .setSubmitcode(submitCode)
                  .setSubmitdatetime(java.time.LocalDateTime.now())
                  .setSolutionid(solutionId)
                  .setResult(SubmissionResult.Processing.value)
                submissionRecord.store()

                (basicInfo._1, basicInfo._2, basicInfo._3, basicInfo._4, submitCode, submissionRecord.getId, solutionId)
              }
            }
          }
      )
      .flatMap { case (contest, language, langVer, challengeSlug, submitCode, submissionId, solutionId) =>
        HackerRankApi[F]()
          .submitAnswer(challengeSlug, contest, language, langVer, submitCode)
          .map((_, submissionId))
      }
      .last
      .map {
        case Some((response, submissionId)) =>
          val client = ChallengeRepository.getInstance(project).getDSLContext
          client.transactionResult { trx =>
            val dsl = DSL.using(trx)
            dsl
              .selectFrom(SOLUTION_SUBMISSION)
              .where(SOLUTION_SUBMISSION.ID.eq(submissionId))
              .fetchOptional()
              .toScala match
              case Some(record) =>
                val result = toSubmissionResult(response.status)
                val message =
                  if result == SubmissionResult.CompilationError then response.compileMessage.getOrElse(response.status)
                  else response.status
                record
                  .setDojosubmissionid(response.id.toString)
                  .setResultdatetime(LocalDateTime.now())
                  .setResult(result.value)
                  .setScore(response.score)
                  .setMessage(message)
                record.store()

                response.codecheckerSignal
                  .zip(response.codecheckerTime)
                  .zip(response.testcaseMessage)
                  .zip(response.testcaseStatus)
                  .map { case (((a, b), c), d) => (a, b, c, d) }
                  .zipWithIndex
                  .foreach { (item, index) =>
                    val (signal, time, message, status) = item
                    val testcaseRecord = dsl
                      .newRecord(HACKERRANK_SUBMISSION_CASE)
                      .setId(IdGenerator.nextId())
                      .setSubmissionid(submissionId)
                      .setTestcasemessage(message)
                      .setNum(index)
                      .setTestcasestatus(status)
                      .setCodecheckersignal(signal)
                      .setCodecheckertime(time.bigDecimal.floatValue())
                    testcaseRecord.store()
                  }
                (result, message)
              case None => throw new Exception("Cannot find submission record")
          }
        case None => throw new IllegalStateException("should not happened")
      }
      .evalTap { case (result, message) =>
        result match
          case SubmissionResult.Success =>
            console.info[F](project, "🎉 Passed!")
          case SubmissionResult.Failure =>
            console.error[F](project, message)
          case SubmissionResult.RuntimeError =>
            console.error[F](project, message)
          case SubmissionResult.CompilationError =>
            console.error[F](project, s"Compilation Error: \n ${message}")
          case SubmissionResult.Processing =>
            console.info[F](project, message)
          case SubmissionResult.Timeout =>
            console.error[F](project, message)
          case SubmissionResult.Unknown =>
            console.error[F](project, message)
      }
      .onFinalizeCase {
        case ExitCase.Succeeded =>
          Async[F].unit
        case ExitCase.Errored(e) =>
          console.error[F](project, s"Error to submit code: \n ${e.getMessage}")
        case ExitCase.Canceled =>
          console.warn[F](project, s"Code submission cancelled for ${vf.getCanonicalPath}")
      }
      .compile
      .drain
  }

  private def queryChallengeBasicInfo(
    item: ChallengeSettingsStateItem,
    client: DSLContext
  ): (HackerRankContest, Language, String, String) = {
    Option(
      client
        .select(
          CHALLENGE.SLUG,
          HACKERRANK_CHALLENGE.CONTESTSLUG,
          CHALLENGE_LANGUAGE.LANGUAGE,
          CHALLENGE_LANGUAGE.LANGUAGEVERSION
        )
        .from(CHALLENGE)
        .innerJoin(HACKERRANK_CHALLENGE)
        .on(CHALLENGE.ID.eq(HACKERRANK_CHALLENGE.ID))
        .innerJoin(CHALLENGE_LANGUAGE)
        .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
        .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
        .fetchOne()
    ).flatMap { record =>
      val challengeSlug = record.get(CHALLENGE.SLUG)
      val contestSlug   = record.get(HACKERRANK_CHALLENGE.CONTESTSLUG)
      val language      = record.get(CHALLENGE_LANGUAGE.LANGUAGE)
      val langVer       = record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION)

      HackerRankContest
        .fromCIString(CIString(contestSlug))
        .zip(Language.fromCIString(CIString(language)))
        .map((_, _, langVer, challengeSlug))
    } match {
      case Some(value) => value
      case None        => throw new Exception("Cannot find data for file")
    }
  }

  private def toSubmissionResult(status: String): SubmissionResult = {
    if status == "Accepted" then SubmissionResult.Success
    else if status == "Wrong Answer" then SubmissionResult.Failure
    else if status == "Compilation error" then CompilationError
    else if status == "Terminated due to timeout" then SubmissionResult.Timeout
    else SubmissionResult.Unknown
  }
}
