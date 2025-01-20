package com.wenjunhuang.codeepiphany.editor.services

import cats.syntax.all.*
import cats.effect.{ Async, Concurrent }
import cats.effect.kernel.Resource.ExitCase
import fs2.Stream
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.jdk.OptionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.{ VirtualFile, VirtualFileUtil }

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.leetcode.model.*
import com.wenjunhuang.codeepiphany.leetcode.model.runCode.LeetCodeRunResult
import com.wenjunhuang.codeepiphany.leetcode.model.submitAnswer.LeetCodeSubmitAnswerResult
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.{ IdGenerator, Tabulator }

object leetcode {
  def runCode[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](
    codeDojo: CodeDojo,
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
      .flatMap { case (dojoId, testCase, language, langVer, challengeSlug) =>
        val extractedCode = language.extractSubmitCode(VirtualFileUtil.readText(vf))
        LeetCodeApi[F](codeDojo)
          .runAnswer(dojoId, challengeSlug, testCase, language, langVer, extractedCode)
          .map((_, testCase))
      }
      .last
      .evalTap {
        case Some((success: LeetCodeRunResult.Success, testCase)) =>
          reportRunResult(codeDojo, project, success, testCase)
        case _ => Async[F].unit
      }
      .onFinalizeCase {
        case ExitCase.Succeeded =>
          Async[F].unit
        case ExitCase.Errored(e) =>
          LoggerFactory[F].getLogger.warn(e)(s"Error to run code of ${codeDojo.value}")
            *>
              console.error[F](project, s"Error to run code")
        case ExitCase.Canceled =>
          console.warn[F](project, s"Code submission cancelled for ${vf.getCanonicalPath}")
      }
      .compile
      .drain
  }

  private def makeResultDiffs(result: LeetCodeRunResult.Success, testCaseInput: String): String = {
    val cases  = StringUtil.splitByLines(testCaseInput).toList
    val header = List("Case", "Your Answer", "Expected Answer")
    val codeAnswer = (cases.zip(
      result.codeAnswer
        .zip(result.expectedCodeAnswer)
    ) ++
      cases.zip(
        result.stdOutputList
          .zip(result.expectedStdOutputList)
      )).filter { case (_, (output, expected)) => output != expected }.map { case (testCase, (output, expected)) =>
      List(testCase, output, expected)
    }

    Tabulator.format(header, codeAnswer)
  }

  private def reportRunResult[F[_]: Async: Concurrent: HttpClientManager](
    codeDojo: CodeDojo,
    project: Project,
    success: LeetCodeRunResult.Success,
    testCase: String
  ) = {
    codeDojo.fromLeetCodeRunResult(success.statusMsg) match
      case SubmissionResult.Success =>
        if success.correctAnswer.contains(true) then console.info[F](project, "🎉 Passed!")
        else
          val msg = makeResultDiffs(success, testCase)
          console.error[F](project, s"Wrong Answer!\n$msg")
      case SubmissionResult.Failure => console.error[F](project, success.statusMsg)
      case SubmissionResult.CompilationError =>
        console.error[F](
          project,
          s"Compilation Error: \n ${success.fullRuntimeError.orElse(success.compileError).getOrElse(success.statusMsg)}"
        )
      case SubmissionResult.Timeout => console.error[F](project, success.statusMsg)
      case SubmissionResult.RuntimeError =>
        console.error[F](
          project,
          s"Runtime Error:\n ${success.fullRuntimeError.orElse(success.runtimeError).getOrElse(success.statusMsg)}"
        )
      case SubmissionResult.Unknown    => console.error[F](project, success.statusMsg)
      case SubmissionResult.Processing => console.info[F](project, "Processing...")
  }

  def submitCode[F[_]: Async: Concurrent: HttpClientManager](
    vf: VirtualFile,
    project: Project,
    item: ChallengeSettingsStateItem
  ): F[Unit] = {
    val codeDojo = item.dojo
    Stream
      .eval(
        ChallengeRepository
          .getInstance(project)
          .getDSLContextResource[F]
          .use { client =>
            Async[F].delay {
              client.transactionResult { trx =>
                val dsl                                                  = DSL.using(trx)
                val (dojoId, testCase, language, langVer, challengeSlug) = queryChallengeBasicInfo(item, dsl)
                val localCode                                            = VirtualFileUtil.readText(vf)
                val submitCode                                           = language.extractSubmitCode(localCode)
                val solutionId = item.solutionId.getOrElse(getOrCreateDefaultSolution(dsl, item.challengeId))

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

                (dojoId, language, langVer, challengeSlug, submitCode, submissionRecord.getId, solutionId)
              }
            }
          }
      )
      .flatMap { case (dojoId, language, langVer, challengeSlug, submitCode, submissionId, solutionId) =>
        LeetCodeApi[F](codeDojo)
          .submitAnswer(dojoId, challengeSlug, language, langVer, submitCode)
          .map((_, submissionId))
      }
      .last
      .map {
        case Some((response: LeetCodeSubmitAnswerResult.Success, submissionId)) =>
          val client = ChallengeRepository.getInstance(project).getDSLContext
          client.transactionResult { trx =>
            val dsl = DSL.using(trx)
            dsl
              .selectFrom(SOLUTION_SUBMISSION)
              .where(SOLUTION_SUBMISSION.ID.eq(submissionId))
              .fetchOptional()
              .toScala match
              case Some(record) =>
                val result = toSubmissionResult(response.statusMsg)
                val message =
                  if result == SubmissionResult.CompilationError then
                    response.fullCompileError.getOrElse(response.statusMsg)
                  else response.statusMsg
                record
                  .setDojosubmissionid(response.submissionId)
                  .setResultdatetime(LocalDateTime.now())
                  .setResult(result.value)
                  .setMessage(message)
                record.store()

                (result, message)
              case _ => throw new Exception("Cannot find submission record")
          }
        case None => throw new IllegalStateException("should not happened")
      }
      .evalTap { case (result, message) =>
        result match
          case SubmissionResult.Success =>
            console.info[F](project, "🎉 Passed!")
          case SubmissionResult.Failure =>
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

  def getOrCreateDefaultSolution(dsl: DSLContext, challengeId: Long): Long = {
    val solutionRecord = dsl
      .selectFrom(SOLUTION)
      .where(SOLUTION.CHALLENGEID.eq(challengeId).and(SOLUTION.ISDEFAULT.eq(1)))
      .fetchOptional()
      .toScala
      .getOrElse {
        val newRecord = dsl
          .newRecord(SOLUTION)
          .setId(IdGenerator.nextId())
          .setChallengeid(challengeId)
          .setTitle("Default")
          .setIsdefault(1)
          .setCreatedatetime(LocalDateTime.now())
        newRecord.store()
        newRecord
      }
    solutionRecord.getId
  }

  private def queryChallengeBasicInfo(
    item: ChallengeSettingsStateItem,
    client: DSLContext
  ): (String, String, Language, LanguageVersion, String) = {
    Option(
      client
        .select(
          CHALLENGE.SLUG,
          CHALLENGE.DOJOID,
          LEETCODE_CHALLENGE.TESTCASE,
          CHALLENGE_LANGUAGE.LANGUAGE,
          CHALLENGE_LANGUAGE.LANGUAGEVERSION
        )
        .from(CHALLENGE)
        .innerJoin(LEETCODE_CHALLENGE)
        .on(CHALLENGE.ID.eq(LEETCODE_CHALLENGE.ID))
        .innerJoin(CHALLENGE_LANGUAGE)
        .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
        .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
        .fetchOne()
    ).flatMap { record =>
      val challengeSlug = record.get(CHALLENGE.SLUG)
      val dojoId        = record.get(CHALLENGE.DOJOID)
      val testCase      = record.get(LEETCODE_CHALLENGE.TESTCASE)
      val language      = record.get(CHALLENGE_LANGUAGE.LANGUAGE)
      val langVer       = record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION)

      Language.fromCIString(CIString(language)).map { lang =>
        (dojoId, testCase, lang, LanguageVersion.fromString(langVer), challengeSlug)
      }
    } match {
      case Some(value) => value
      case None        => throw new Exception("Cannot find data for file")
    }
  }

  private def toSubmissionResult(status: String): SubmissionResult = {
    if status == "Accepted" then SubmissionResult.Success
    else if status == "Wrong Answer" then SubmissionResult.Failure
    else if status == "Compile error" then SubmissionResult.CompilationError
    else if status == "Time Limit Exceeded" then SubmissionResult.Timeout
    else SubmissionResult.Unknown
  }
}
