package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.leetcode.models.*
import com.wenjunhuang.codeepiphany.leetcode.models.submitAnswer.LeetCodeSubmitAnswerResult
import com.wenjunhuang.codeepiphany.leetcode.models.submitAnswer.LeetCodeSubmitAnswerResult.{Pending, Started, Success}
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.SubmissionResult.Processing
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.services.{BaseSubmissionService, ChallengeRepository, console}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.Tabulator
import fs2.Stream
import org.jooq.{DSLContext, Record}
import org.typelevel.ci.CIString

import scala.jdk.OptionConverters.*

class LeetCodeSubmissionService(
  project: Project,
  private val myLeetCode: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
) extends BaseSubmissionService(project, myLeetCode) {
  override type SubmissionRequest  = LeetCodeSubmissionRequest
  override type SubmissionResponse = LeetCodeSubmitAnswerResult

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): IO[LeetCodeSubmissionRequest] = {
    ChallengeRepository
      .getInstance(project)
      .getDSLContextResource
      .use(client => IO.delay(createSubmissionRequest(item, client)))
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
  ):Stream[IO, LeetCodeSubmitAnswerResult] =
    LeetCodeApi(myLeetCode).submitAnswer(
      basicInfo.questionId,
      basicInfo.questionSlug,
      basicInfo.language,
      basicInfo.languageVersion,
      processedCode
    )

  override protected def reportSubmitResult(
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: LeetCodeSubmitAnswerResult
  ): IO[Unit] =
    lastResponseInfo.result match {
      case SubmissionResult.Success =>
        console.info(project, PluginBundle.message("submission.passed") + s"\n${lastResponseInfo.message}")
      case _ =>
        console.error(project, s"${lastResponseInfo.result.show}\n${lastResponseInfo.message}")
    }

  private def formatSuccessMetrics(response: LeetCodeSubmitAnswerResult.Success): String = {
    Tabulator.format(
      List("Metric", "Value"),
      List("Runtime", f"${response.statusRuntime} (Beats ${response.runtimePercentile.getOrElse(0.0f)}%.2f%%)"),
      List("Memory", f"${response.statusMemory} (Beats ${response.memoryPercentile.getOrElse(0.0f)}%.2f%%)")
    )
  }

  private def formatFailureDetails(response: LeetCodeSubmitAnswerResult.Success): String = {
    s"""
       |-------------------------
       |${PluginBundle.message("leetcode.submissionResult.wrongAnswer.input.text")}:
       |${response.input.getOrElse("")}
       |-------------------------
       |${PluginBundle.message("leetcode.submissionResult.wrongAnswer.output.text")}:
       |${response.codeOutput.getOrElse("")}
       |-------------------------
       |${PluginBundle.message("leetcode.submissionResult.wrongAnswer.expected.text")}:
       |${response.expectedOutput.getOrElse("")}
       |""".stripMargin
//    Tabulator.format(
//      List("Input", "Output", "Expected"),
//      List(response.input, response.codeOutput, response.expectedOutput)
//        .map(_.getOrElse(""))
//        .map(StringUtil.escapeLineBreak)
//    )
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
