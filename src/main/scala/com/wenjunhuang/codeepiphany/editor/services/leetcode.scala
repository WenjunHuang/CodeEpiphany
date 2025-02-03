package com.wenjunhuang.codeepiphany.editor.services

import cats.effect.{ Async, Concurrent }
import cats.effect.kernel.Resource.ExitCase
import cats.syntax.all.*
import fs2.Stream
import java.time.LocalDateTime
import org.jooq.impl.DSL
import org.jooq.{ DSLContext, Record }
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

class LeetCodeService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory] {
  type LeetCodeDojo = CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type

  def runCode(vf: VirtualFile, project: Project, item: ChallengeSettingsStateItem): F[Unit] = {
    item.dojo match {
      case codeDojo: LeetCodeDojo =>
        for {
          challengeInfo <- fetchChallengeInfo(project, item)
          localCode     <- readLocalCode(codeDojo, vf, challengeInfo.language)
          processedCode <- extractCode(codeDojo, localCode, challengeInfo.language)
          _ <- executeLeetCodeRun(codeDojo, challengeInfo, processedCode)
            .evalMap(response => handleRunResult(codeDojo, project, response, challengeInfo.testCase))
            .compile
            .drain
        } yield ()
      case _ =>
        Async[F].raiseError(new IllegalArgumentException("Unsupported LeetCode platform"))
    }
  }

  def submitCode(vf: VirtualFile, project: Project, item: ChallengeSettingsStateItem): F[Unit] = item.dojo match {
    case codeDojo @ (CodeDojo.LeetCode | CodeDojo.LeetCodeCN) =>
      for {
        localCode     <- readLocalCode(codeDojo, vf, item.language)
        processedCode <- extractCode(codeDojo, localCode, item.language)
        challengeInfo <- fetchChallengeInfo(project, item)
        submissionId  <- storeSubmission(project, item, localCode, processedCode)
        _             <- executeLeetCodeSubmit(project, codeDojo, challengeInfo, processedCode, submissionId)
      } yield ()
    case _ =>
      Async[F].raiseError(new IllegalArgumentException("Unsupported LeetCode platform"))
  }

  private def executeLeetCodeSubmit(
    project: Project,
    codeDojo: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type,
    info: LeetCodeChallengeInfo,
    code: String,
    submissionId: Long
  ) = {
    LeetCodeApi[F](codeDojo)
      .submitAnswer(info.dojoId, info.challengeSlug, info.language, info.langVer, code)
      .evalMap(apiResponse =>
        updateSubmissionRecord(project, codeDojo, submissionId, apiResponse) >> Async[F].pure(apiResponse)
      )
      .compile
      .last
      .flatMap {
        case Some(response: submitAnswer.LeetCodeSubmitAnswerResult.Success) =>
          reportSubmitResult(project, codeDojo, response)
        case _ => Async[F].unit
      }
      .handleErrorWith(error => updateSubmissionOnError(project, submissionId, error) >> Async[F].raiseError(error))
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

  private def fetchChallengeInfo(project: Project, item: ChallengeSettingsStateItem): F[LeetCodeChallengeInfo] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use(client => Async[F].delay(queryChallengeInfo(item, client)))
  }

  private def readLocalCode(codeDojo: LeetCodeDojo, vf: VirtualFile, language: Language): F[String] =
    Async[F].blocking {
      VirtualFileUtil.readText(vf)
    }

  private def extractCode(codeDojo: LeetCodeDojo, rawCode: String, language: Language): F[String] =
    Async[F].delay {
      if codeDojo.requiresCodeRegionEnclosure then language.extractCodeFromRegion(rawCode)
      else rawCode
    }

  private def executeLeetCodeRun(
    codeDojo: LeetCodeDojo,
    info: LeetCodeChallengeInfo,
    code: String
  ): Stream[F, LeetCodeRunResult] = {
    LeetCodeApi[F](codeDojo)
      .runAnswer(info.dojoId, info.challengeSlug, info.testCase, info.language, info.langVer, code)
  }

  private def storeSubmission(
    project: Project,
    item: ChallengeSettingsStateItem,
    localCode: String,
    processedCode: String
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
              .setSubmitcode(processedCode)
              .setSubmitdatetime(LocalDateTime.now())
              .setSolutionid(item.solutionId)
              .setResult(SubmissionResult.Processing.value)
            record.store()
            record.getId
          }
        }
      }
  }

  private def updateSubmissionRecord(
    project: Project,
    codeDojo: CodeDojo,
    submissionId: Long,
    response: LeetCodeSubmitAnswerResult
  ): F[Unit] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use { dsl =>
        Async[F].delay {
          dsl.transaction { trx =>
            response match
              case success: LeetCodeSubmitAnswerResult.Success =>
                val ctx = DSL.using(trx)
                updateMainSubmission(ctx, codeDojo, submissionId, success)
                updateLeetCodeSpecificData(ctx, submissionId, success)
              case _ =>
          }
        }
      }
  }

  private def handleRunResult(
    codeDojo: CodeDojo,
    project: Project,
    result: LeetCodeRunResult,
    testCase: String
  ): F[Unit] = result match {
    case success: LeetCodeRunResult.Success =>
      handleRunSuccess(codeDojo, project, success, testCase)
    case _ => Async[F].unit
  }

  private def queryChallengeInfo(item: ChallengeSettingsStateItem, client: DSLContext): LeetCodeChallengeInfo = {
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
      .fetchOptional()
      .toScala
      .flatMap(parseLeetCodeRecord)
      .getOrElse(throw new IllegalStateException("LeetCode challenge data not found"))
  }

  private def parseLeetCodeRecord(record: Record): Option[LeetCodeChallengeInfo] = {
    for {
      dojoId <- Option(record.get(CHALLENGE.DOJOID))
      testCase = Option(record.get(LEETCODE_CHALLENGE.TESTCASE))
      language <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      slug <- Option(record.get(CHALLENGE.SLUG))
    } yield LeetCodeChallengeInfo(dojoId, testCase.getOrElse(""), language, langVer, slug)
  }

  private def updateMainSubmission(
    ctx: DSLContext,
    codeDojo: CodeDojo,
    submissionId: Long,
    response: LeetCodeSubmitAnswerResult.Success
  ): Unit = {
    val result = codeDojo.fromLeetCodeRunResult(response.statusMsg, None)
    val msg    = formatErrorMessage(result, response)
    ctx
      .update(SOLUTION_SUBMISSION)
      .set(SOLUTION_SUBMISSION.DOJOSUBMISSIONID, response.submissionId)
      .set(SOLUTION_SUBMISSION.RESULTDATETIME, LocalDateTime.now())
      .set(SOLUTION_SUBMISSION.RESULT, result.value)
      .set(SOLUTION_SUBMISSION.MESSAGE, msg)
      .where(SOLUTION_SUBMISSION.ID.eq(submissionId))
      .execute()
  }

  private def updateLeetCodeSpecificData(
    ctx: DSLContext,
    submissionId: Long,
    response: LeetCodeSubmitAnswerResult.Success
  ): Unit = {
    val record = ctx
      .newRecord(LEETCODE_SUBMISSION)
      .setId(submissionId)
      .setExpectedoutput(response.expectedOutput.orNull)
      .setInputformatted(response.inputFormatted.orNull)
      .setLasttestcase(response.lastTestcase.orNull)
      .setMemory(response.memory)
      .setMemorypercentile(response.memoryPercentile.map(float2Float).orNull)
      .setRuntimepercentile(response.runtimePercentile.map(float2Float).orNull)
      .setStatusmemory(response.statusMemory)
      .setTotalcorrect(response.totalCorrect.map(int2Integer).orNull)
      .setTotaltestcases(response.totalTestcases.map(int2Integer).orNull)
      .setStatusruntime(response.statusRuntime)
      .setCodeoutput(response.codeOutput.orNull)
      .setStdoutput(response.stdOutput.orNull)
    record.store()
  }

  private def handleRunSuccess(
    codeDojo: CodeDojo,
    project: Project,
    success: LeetCodeRunResult.Success,
    testCase: String
  ): F[Unit] = {
    codeDojo.fromLeetCodeRunResult(success.statusMsg,success.correctAnswer) match {
      case SubmissionResult.Success if success.correctAnswer.contains(true) =>
        console.info[F](project, "🎉 Passed!")
      case SubmissionResult.Success =>
        console.error[F](project, s"Wrong Answer!\n${formatResultDiff(success, testCase)}")
      case result =>
        console.error[F](project, formatErrorMessage(result, success))
    }
  }

  private def formatResultDiff(result: LeetCodeRunResult.Success, testCase: String): String = {
    val cases = StringUtil.splitByLines(testCase).toList
    val comparisons = cases.zip(result.codeAnswer.zip(result.expectedCodeAnswer)) ++
      cases.zip(result.stdOutputList.zip(result.expectedStdOutputList))

    Tabulator.format(
      List("Case", "Your Answer", "Expected Answer") +:
        comparisons.collect {
          case (testCase, (output, expected)) if output != expected =>
            List(
              StringUtil.escapeLineBreak(testCase),
              StringUtil.escapeLineBreak(output),
              StringUtil.escapeLineBreak(expected)
            )
        }
    )
  }

  private def formatErrorMessage(result: SubmissionResult, response: LeetCodeRunResult.Success): String =
    result match {
      case SubmissionResult.CompilationError =>
        response.fullCompileError.orElse(response.compileError).getOrElse(response.statusMsg)
      case SubmissionResult.RuntimeError =>
        response.fullRuntimeError.orElse(response.runtimeError).getOrElse(response.statusMsg)
      case _ =>
        response.statusMsg
    }

  private def formatErrorMessage(result: SubmissionResult, response: LeetCodeSubmitAnswerResult.Success): String =
    result match {
      case SubmissionResult.CompilationError =>
        response.fullCompileError.orElse(response.compileError).getOrElse(response.statusMsg)
      case SubmissionResult.RuntimeError =>
        response.fullRuntimeError.orElse(response.runtimeError).getOrElse(response.statusMsg)
      case _ =>
        response.statusMsg
    }

  private def reportSubmitResult(
    project: Project,
    codeDojo: CodeDojo,
    response: LeetCodeSubmitAnswerResult.Success
  ): F[Unit] =
    val result = codeDojo.fromLeetCodeRunResult(response.statusMsg,None)
    result match {
      case SubmissionResult.Success =>
        console.info[F](project, formatSuccessMetrics(response))
      case SubmissionResult.Failure =>
        console.error[F](project, formatFailureDetails(response))
      case _ =>
        console.error[F](project, formatErrorMessage(result, response))
    }

  private def formatSuccessMetrics(response: LeetCodeSubmitAnswerResult.Success): String = {
    Tabulator.format(
      List("Metric", "Value"),
      List(
        List("Runtime", f"${response.statusRuntime} (Top ${response.runtimePercentile.getOrElse(0.0f)}%2f%%)"),
        List("Memory", f"${response.statusMemory} (Top ${response.memoryPercentile.getOrElse(0.0f)}%2f%%)")
      )
    )
  }

  private def formatFailureDetails(response: LeetCodeSubmitAnswerResult.Success): String = {
    Tabulator.format(
      List("Input", "Output", "Expected"),
      List(response.input, response.codeOutput, response.expectedOutput)
        .map(_.getOrElse(""))
        .map(StringUtil.escapeLineBreak)
    )
  }

  private case class LeetCodeChallengeInfo(
    dojoId: String,
    testCase: String,
    language: Language,
    langVer: LanguageVersion,
    challengeSlug: String
  )
}

object leetcode {
  def runCode[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](
    codeDojo: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type,
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
        val extractedCode = language.extractCodeFromRegion(VirtualFileUtil.readText(vf))
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
      List(
        StringUtil.escapeLineBreak(testCase),
        StringUtil.escapeLineBreak(output),
        StringUtil.escapeLineBreak(expected)
      )
    }

    Tabulator.format((header +: codeAnswer)*)
  }

  private def reportRunResult[F[_]: Async: Concurrent: HttpClientManager](
    codeDojo: CodeDojo,
    project: Project,
    success: LeetCodeRunResult.Success,
    testCase: String
  ) = {
    codeDojo.fromLeetCodeRunResult(success.statusMsg,success.correctAnswer) match
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

  private def reportSubmitResult[F[_]: Async: Concurrent: HttpClientManager](
    project: Project,
    result: SubmissionResult,
    success: LeetCodeSubmitAnswerResult.Success
  ) = {
    result match
      case SubmissionResult.Success =>
        val msg = Tabulator.format(
          List("Runtime", "Memory"),
          List(
            f"${success.statusRuntime} defeated ${success.runtimePercentile.getOrElse(0.0f)}%.2f%%",
            f"${success.statusMemory} defeated ${success.memoryPercentile.getOrElse(0.0f)}%.2f%%"
          )
        )
        console.info[F](project, s"🎉 Passed!\n$msg")
      case SubmissionResult.Failure =>
        val msg = Tabulator.format(
          List("Input", "Output", "Expected"),
          List(
            s"${StringUtil.escapeLineBreak(success.input.getOrElse(""))}",
            s"${StringUtil.escapeLineBreak(success.codeOutput.getOrElse(""))}",
            s"${StringUtil.escapeLineBreak(success.expectedOutput.getOrElse(""))}"
          )
        )
        console.error[F](project, s"${success.statusMsg}\n$msg")
      case SubmissionResult.CompilationError =>
        console.error[F](
          project,
          s"Compilation Error: \n ${success.fullCompileError.orElse(success.compileError).getOrElse(success.statusMsg)}"
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
    item.dojo match
      case codeDojo @ (CodeDojo.LeetCode | CodeDojo.LeetCodeCN) =>
        Stream
          .eval(
            ChallengeRepository
              .getInstance(project)
              .getDSLContextResource[F]
              .use { client =>
                Async[F].delay {
                  client.transactionResult { trx =>
                    val dsl                                           = DSL.using(trx)
                    val (dojoId, _, language, langVer, challengeSlug) = queryChallengeBasicInfo(item, dsl)
                    val localCode                                     = VirtualFileUtil.readText(vf)
                    val submitCode                                    = language.extractCodeFromRegion(localCode)
                    val solutionId                                    = item.solutionId

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
                    val result = codeDojo.fromLeetCodeRunResult(response.statusMsg,None)
                    val message = result match
                      case SubmissionResult.Success | SubmissionResult.Failure | SubmissionResult.Timeout |
                          SubmissionResult.Failure | SubmissionResult.Unknown | SubmissionResult.Processing =>
                        response.statusMsg
                      case SubmissionResult.CompilationError =>
                        response.fullCompileError.orElse(response.compileError).getOrElse(response.statusMsg)
                      case SubmissionResult.RuntimeError =>
                        response.fullRuntimeError.orElse(response.runtimeError).getOrElse(response.statusMsg)

                    record
                      .setDojosubmissionid(response.submissionId)
                      .setResultdatetime(LocalDateTime.now())
                      .setResult(result.value)
                      .setMessage(message)
                    record.store()

                    dsl
                      .selectFrom(LEETCODE_SUBMISSION)
                      .where(LEETCODE_SUBMISSION.ID.eq(submissionId))
                      .fetchOptional()
                      .toScala
                      .getOrElse(dsl.newRecord(LEETCODE_SUBMISSION).setId(record.getId))
                      .setExpectedoutput(response.expectedOutput.orNull)
                      .setInputformatted(response.inputFormatted.orNull)
                      .setLasttestcase(response.lastTestcase.orNull)
                      .setMemory(response.memory)
                      .setMemorypercentile(response.memoryPercentile.map(float2Float).orNull)
                      .setRuntimepercentile(response.runtimePercentile.map(float2Float).orNull)
                      .setStatusmemory(response.statusMemory)
                      .setTotalcorrect(response.totalCorrect.map(int2Integer).orNull)
                      .setTotaltestcases(response.totalTestcases.map(int2Integer).orNull)
                      .setStatusruntime(response.statusRuntime)
                      .setCodeoutput(response.codeOutput.orNull)
                      .setStdoutput(response.stdOutput.orNull)
                      .store()

                    (result, response)
                  case _ => throw new IllegalStateException("Cannot find submission record")
              }
            case _ => throw new IllegalStateException("should not happened")
          }
          .evalTap { case (result, response) =>
            reportSubmitResult(project, result, response)
          }
          .onFinalizeCase {
            case ExitCase.Succeeded =>
              Async[F].unit
            case ExitCase.Errored(e) =>
              console.error[F](project, s"Error to submit code")
            case ExitCase.Canceled =>
              console.warn[F](project, s"Code submission cancelled for ${vf.getCanonicalPath}")
          }
          .compile
          .drain
      case _ =>
        Async[F].raiseError(new IllegalStateException("Challenge's dojo type is neither LeetCode nor LeetCodeCN"))
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
}
