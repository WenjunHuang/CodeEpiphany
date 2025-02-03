package com.wenjunhuang.codeepiphany.editor.services

import cats.effect.{ Async, Concurrent }
import cats.effect.kernel.Resource.ExitCase
import cats.syntax.all.*
import fs2.Stream
import java.time.LocalDateTime
import org.jooq.impl.DSL
import org.jooq.DSLContext
import org.jooq.Record
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger
import scala.jdk.OptionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{ VirtualFile, VirtualFileUtil }

import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesSubmissionResponse
import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesApi
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettingsConfigurable
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.IdGenerator

object CodeForcesService {
  def submitCode[F[_]: Async: Concurrent: HttpClientManager: Logger](
    vf: VirtualFile,
    project: Project,
    item: ChallengeSettingsStateItem
  ): F[Unit] = item.dojo match {
    case CodeDojo.CodeForces => submissionFlow(vf, project, item)
    case _                   => Async[F].raiseError(new IllegalArgumentException("Unsupported code dojo"))
  }

  private def submissionFlow[F[_]: Async: Concurrent: HttpClientManager: Logger](
    vf: VirtualFile,
    project: Project,
    item: ChallengeSettingsStateItem
  ): F[Unit] = {
    for {
      localCode    <- readFileContent(vf)
      basicInfo    <- fetchBasicInfo(project, item)
      submitCode   <- processCode(localCode, basicInfo.language)
      programType  <- resolveProgramType(basicInfo.language, basicInfo.langVer)
      submissionId <- storeSubmissionRecord(project, item, localCode, submitCode)
      _ <- submitToCodeForces(basicInfo, submitCode, programType)
        .evalMap(response => handleSubmissionResponse(project, submissionId, response))
        .compile
        .drain
        .handleErrorWith(e => updateSubmissionOnError(project, submissionId, e) >> Async[F].raiseError(e))
    } yield ()
  }

  private def readFileContent[F[_]: Async](vf: VirtualFile): F[String] =
    Async[F].blocking(VirtualFileUtil.readText(vf))

  private def fetchBasicInfo[F[_]: Async](
    project: Project,
    item: ChallengeSettingsStateItem
  ): F[CFChallengeBasicInfo] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use(client => Async[F].delay(queryBasicInfo(item, client)))
  }

  private def processCode[F[_]: Async](rawCode: String, language: Language): F[String] = Async[F].delay {
    if (CodeDojo.CodeForces.requiresCodeRegionEnclosure)
      language.extractCodeFromRegion(rawCode)
    else
      rawCode
  }

  private def resolveProgramType[F[_]: Async](language: Language, version: LanguageVersion): F[String] =
    Async[F].fromOption(
      CodeForcesSettingsConfigurable.CODEFORCES_LANGUAGES.find { case (lang, ver, _) =>
        lang == language && ver == version
      }
        .map(_._3),
      new IllegalStateException(s"Unsupported language: $language $version")
    )

  private def storeSubmissionRecord[F[_]: Async](
    project: Project,
    item: ChallengeSettingsStateItem,
    localCode: String,
    submitCode: String
  ): F[Long] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use { dsl =>
        Async[F].delay {
          dsl.transactionResult { trx =>
            val ctx = DSL.using(trx)
            val record = ctx
              .newRecord(SOLUTION_SUBMISSION)
              .setId(IdGenerator.nextId())
              .setChallengelanguageid(item.challengeLanguageId)
              .setLocalcode(localCode)
              .setSubmitcode(submitCode)
              .setSubmitdatetime(LocalDateTime.now())
              .setSolutionid(item.solutionId)
              .setResult(SubmissionResult.Processing.value)
            record.store()
            record.getId
          }
        }
      }
  }

  private def submitToCodeForces[F[_]: Async: HttpClientManager](
    basicInfo: CFChallengeBasicInfo,
    code: String,
    programTypeId: String
  ): Stream[F, CodeForcesSubmissionResponse] = {
    CodeForcesApi[F]().submitAnswer(basicInfo.contestId, basicInfo.index, basicInfo.problemsetName, programTypeId, code)
  }

  private def handleSubmissionResponse[F[_]: Async](
    project: Project,
    submissionId: Long,
    response: CodeForcesSubmissionResponse
  ): F[Unit] = {
    updateSubmissionRecord(project, submissionId, response) >>
      logResult(project, response.result, response.message)
  }

  private def updateSubmissionRecord[F[_]: Async](
    project: Project,
    submissionId: Long,
    response: CodeForcesSubmissionResponse
  ): F[Unit] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use { dsl =>
        Async[F].delay {
          dsl.transaction { trx =>
            DSL
              .using(trx)
              .update(SOLUTION_SUBMISSION)
              .set(SOLUTION_SUBMISSION.DOJOSUBMISSIONID, response.submissionId.toString)
              .set(SOLUTION_SUBMISSION.RESULTDATETIME, LocalDateTime.now())
              .set(SOLUTION_SUBMISSION.RESULT, response.result.value)
              .set(SOLUTION_SUBMISSION.MESSAGE, response.message)
              .where(SOLUTION_SUBMISSION.ID.eq(submissionId))
              .execute()
          }
        }
      }
  }

  private def logResult[F[_]: Async](project: Project, result: SubmissionResult, message: String): F[Unit] =
    result match {
      case SubmissionResult.Success =>
        console.info[F](project, s"🎉 Passed!\n$message")
      case SubmissionResult.Processing =>
        console.info[F](project, s"Processing Submission $message ... ")
      case _ =>
        console.error[F](project, s"${result.toString.toUpperCase}: $message")
    }

  private def updateSubmissionOnError[F[_]: Async](project: Project, submissionId: Long, error: Throwable): F[Unit] = {
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

  private def handleSubmissionError[F[_]: Async: Logger](project: Project, error: Throwable): F[Unit] = {
    Logger[F].warn(error)("Error to submit code") *>
      console.error[F](project, s"Submission failed: ${error.getMessage}")
  }

  private def queryBasicInfo(item: ChallengeSettingsStateItem, client: DSLContext): CFChallengeBasicInfo = {
    client
      .select(
        CHALLENGE.DOJOID,
        CODEFORCES_CHALLENGE.CONTESTID,
        CODEFORCES_CHALLENGE.INDEX,
        CODEFORCES_CHALLENGE.PROBLEMSETNAME,
        CHALLENGE_LANGUAGE.LANGUAGE,
        CHALLENGE_LANGUAGE.LANGUAGEVERSION
      )
      .from(CHALLENGE)
      .innerJoin(CODEFORCES_CHALLENGE)
      .on(CHALLENGE.ID.eq(CODEFORCES_CHALLENGE.ID))
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
      .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
      .fetchOptional()
      .toScala
      .flatMap(parseCodeForcesRecord)
      .getOrElse(throw new Exception("Cannot find challenge data"))
  }

  private def parseCodeForcesRecord(record: Record): Option[CFChallengeBasicInfo] = {
    for {
      contestId <- Option(record.get(CODEFORCES_CHALLENGE.CONTESTID)).map(_.toLong)
      index     <- Option(record.get(CODEFORCES_CHALLENGE.INDEX))
      problemset = Option(record.get(CODEFORCES_CHALLENGE.PROBLEMSETNAME))
      language <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
    } yield CFChallengeBasicInfo(contestId, index, problemset, language, langVer)
  }

  private case class CFChallengeBasicInfo(
    contestId: Long,
    index: String,
    problemsetName: Option[String],
    language: Language,
    langVer: LanguageVersion
  )
}

object codeforces {
  def submitCode[F[_]: Async: Concurrent: HttpClientManager](
    vf: VirtualFile,
    project: Project,
    item: ChallengeSettingsStateItem
  ): F[Unit] = {
    item.dojo match
      case CodeDojo.CodeForces =>
        Stream
          .eval(for
            localCode <- Async[F].blocking { VirtualFileUtil.readText(vf) }
            result <- ChallengeRepository
              .getInstance(project)
              .getDSLContextResource[F]
              .use { client =>
                Async[F].delay {
                  client.transactionResult { trx =>
                    val dsl                                                   = DSL.using(trx)
                    val (contestId, index, problemsetName, language, langVer) = queryChallengeBasicInfo(item, dsl)
                    val submitCode =
                      if CodeDojo.CodeForces.requiresCodeRegionEnclosure then language.extractCodeFromRegion(localCode)
                      else localCode
                    val programTypeId = CodeForcesSettingsConfigurable.CODEFORCES_LANGUAGES
                      .find(p => p._1 == language && p._2 == langVer)
                      .map(_._3)
                      .getOrElse(
                        throw new IllegalStateException(
                          s"Cannot find CodeForces program type id for $language $langVer"
                        )
                      )

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

                    (submissionRecord.getId, contestId, index, problemsetName, submitCode, programTypeId)
                  }
                }
              }
          yield result)
          .flatMap { case (submissionId, contestId, index, problemsetName, submitCode, programTypeId) =>
            CodeForcesApi[F]()
              .submitAnswer(contestId, index, problemsetName, programTypeId, submitCode)
              .map((submissionId, _))
              .onFinalizeCase {
                case ExitCase.Errored(e) =>
                  ChallengeRepository
                    .getInstance(project)
                    .getDSLContextResource[F]
                    .use { dsl =>
                      Async[F].delay {
                        dsl
                          .update(SOLUTION_SUBMISSION)
                          .set(SOLUTION_SUBMISSION.RESULT, SubmissionResult.Failure.value)
                          .set(SOLUTION_SUBMISSION.MESSAGE, e.getMessage)
                          .where(SOLUTION_SUBMISSION.ID.eq(submissionId))
                          .execute()
                      }
                    }
                case _ => Async[F].unit
              }
          }
          .last
          .map {
            case Some((submissionId, response)) =>
              val client = ChallengeRepository.getInstance(project).getDSLContext
              client.transactionResult { trx =>
                val dsl = DSL.using(trx)
                dsl
                  .selectFrom(SOLUTION_SUBMISSION)
                  .where(SOLUTION_SUBMISSION.ID.eq(submissionId))
                  .fetchOptional()
                  .toScala match
                  case Some(record) =>
                    record
                      .setDojosubmissionid(response.submissionId.toString)
                      .setResultdatetime(LocalDateTime.now())
                      .setResult(response.result.value)
                      .setMessage(response.message)
                    record.store()
                    (response.result, response.message)
                  case _ => throw new IllegalStateException("Cannot find submission record")
              }
            case _ => throw new IllegalStateException("should not happened")
          }
          .evalTap { case (result, message) =>
            result match
              case SubmissionResult.Success =>
                console.info[F](project, s"🎉 Passed!\n${message}")
              case SubmissionResult.Failure =>
                console.error[F](project, message)
              case SubmissionResult.RuntimeError =>
                console.error[F](project, message)
              case SubmissionResult.CompilationError =>
                console.error[F](project, message)
              case SubmissionResult.Processing =>
                console.info[F](project, message)
              case SubmissionResult.Timeout =>
                console.error[F](project, message)
              case SubmissionResult.Unknown =>
                console.error[F](project, message)
          }
          .compile
          .drain
      case _ =>
        Async[F].raiseError(new IllegalStateException("The dojo type of the challenge is not CodeForces"))
  }

  private def queryChallengeBasicInfo(
    item: ChallengeSettingsStateItem,
    client: DSLContext
  ): (Long, String, Option[String], Language, LanguageVersion) = {
    client
      .select(
        CHALLENGE.DOJOID,
        CODEFORCES_CHALLENGE.CONTESTID,
        CODEFORCES_CHALLENGE.INDEX,
        CODEFORCES_CHALLENGE.PROBLEMSETNAME,
        CHALLENGE_LANGUAGE.LANGUAGE,
        CHALLENGE_LANGUAGE.LANGUAGEVERSION
      )
      .from(CHALLENGE)
      .innerJoin(CODEFORCES_CHALLENGE)
      .on(CHALLENGE.ID.eq(CODEFORCES_CHALLENGE.ID))
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
      .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
      .fetchOptional()
      .toScala
      .flatMap { record =>
        val contestId: Long = Option(record.get(CODEFORCES_CHALLENGE.CONTESTID)).map(_.toLong).getOrElse(99999L)
        val index           = record.get(CODEFORCES_CHALLENGE.INDEX)
        val problemsetName  = Option(record.get(CODEFORCES_CHALLENGE.PROBLEMSETNAME))
        val language        = Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
        val langVer         = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))

        language.map { l => (contestId, index, problemsetName, l, langVer) }
      } match {
      case Some(value) => value
      case None        => throw new Exception("Cannot find data for file")
    }
  }
}
