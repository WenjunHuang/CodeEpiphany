package com.wenjunhuang.codeepiphany.editor.services

import cats.syntax.all.*
import cats.effect.Concurrent
import cats.effect.kernel.Async
import org.jooq.{DSLContext, Record}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project
import scala.jdk.OptionConverters.*

import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.leetcode.model.submitAnswer.LeetCodeSubmitAnswerResult
import com.wenjunhuang.codeepiphany.model.{ChallengeRepository, CodeDojo, Language, LanguageVersion, SubmissionResult}
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.leetcode.model.submitAnswer.LeetCodeSubmitAnswerResult.{Pending, Started, Success}
import com.wenjunhuang.codeepiphany.leetcode.model.*
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.SubmissionResult.Processing
import com.wenjunhuang.codeepiphany.services.{console, BaseSubmissionService}
import com.wenjunhuang.codeepiphany.utils.Tabulator

class LeetCodeSubmissionService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](
  project: Project,
  private val myLeetCode: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
) extends BaseSubmissionService[F](project, myLeetCode) {
  override type SubmissionRequest  = LeetCodeSubmissionRequest
  override type SubmissionResponse = LeetCodeSubmitAnswerResult

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): F[LeetCodeSubmissionRequest] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource[F]
      .use(client => Async[F].delay(createSubmissionRequest(item, client)))
  }

  override protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: LeetCodeSubmitAnswerResult
  ): SubmissionResponseInfo = {
    response match
      case started: Started =>
        SubmissionResponseInfo(Processing, "", started.leetCodeSubmissionId)
      case pending: Pending =>
        SubmissionResponseInfo(Processing, "", pending.leetCodeSubmissionId)
      case success: Success =>
        val record = dsl
          .newRecord(LEETCODE_SUBMISSION)
          .setId(submissionId.value)
          .setExpectedoutput(success.expectedOutput.orNull)
          .setInputformatted(success.inputFormatted.orNull)
          .setLasttestcase(success.lastTestcase.orNull)
          .setMemory(success.memory)
          .setMemorypercentile(success.memoryPercentile.map(float2Float).orNull)
          .setRuntimepercentile(success.runtimePercentile.map(float2Float).orNull)
          .setStatusmemory(success.statusMemory)
          .setTotalcorrect(success.totalCorrect.map(int2Integer).orNull)
          .setTotaltestcases(success.totalTestcases.map(int2Integer).orNull)
          .setStatusruntime(success.statusRuntime)
          .setCodeoutput(success.codeOutput.orNull)
          .setStdoutput(success.stdOutput.orNull)
        record.store()

        val result = myLeetCode.fromLeetCodeRunResult(success.statusMsg, None)
        val msg = result match
          case SubmissionResult.Success =>
            formatSuccessMetrics(success)
          case SubmissionResult.Failure =>
            formatFailureDetails(success)
          case _ =>
            formatErrorMessage(result, success)
        SubmissionResponseInfo(result, msg, success.submissionId)
  }

  override protected def callApi(
    basicInfo: LeetCodeSubmissionRequest,
    processedCode: String
  ): fs2.Stream[F, LeetCodeSubmitAnswerResult] =
    LeetCodeApi[F](myLeetCode).submitAnswer(
      basicInfo.questionId,
      basicInfo.questionSlug,
      basicInfo.language,
      basicInfo.languageVersion,
      processedCode
    )

  override protected def reportSubmitResult(
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: LeetCodeSubmitAnswerResult
  ): F[Unit] =
    lastResponseInfo.result match {
      case SubmissionResult.Success =>
        console.info[F](project, s"${SubmissionResult.Success.show}\n${lastResponseInfo.message}")
      case _ =>
        console.error[F](project, s"${lastResponseInfo.result.show}\n${lastResponseInfo.message}")
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

  private def createSubmissionRequest(
    item: ChallengeSettingsStateItem,
    client: DSLContext
  ): LeetCodeSubmissionRequest = {
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

  private def parseLeetCodeRecord(record: Record): Option[LeetCodeSubmissionRequest] = {
    for {
      dojoId   <- Option(record.get(CHALLENGE.DOJOID))
      language <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      slug <- Option(record.get(CHALLENGE.SLUG))
    } yield LeetCodeSubmissionRequest(dojoId, slug, language, langVer)
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
  case class LeetCodeSubmissionRequest(
    questionId: String,
    questionSlug: String,
    language: Language,
    languageVersion: LanguageVersion
  )
}
