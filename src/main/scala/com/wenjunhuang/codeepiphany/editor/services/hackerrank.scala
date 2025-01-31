package com.wenjunhuang.codeepiphany.editor.services

import cats.effect.{ Async, Concurrent }
import cats.effect.kernel.Resource.ExitCase
import fs2.Stream
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.typelevel.ci.CIString
import scala.jdk.OptionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{ VirtualFile, VirtualFileUtil }

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.hackerrank.model.HackerRankContest
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, Language, SubmissionResult }
import com.wenjunhuang.codeepiphany.model.SubmissionResult.CompilationError
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.IdGenerator

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
          .use { dsl =>
            Async[F].delay { queryChallengeBasicInfo(item, dsl) }
          }
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
