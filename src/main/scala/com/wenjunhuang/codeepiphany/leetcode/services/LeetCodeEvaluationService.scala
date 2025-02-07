package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.{Async, Concurrent}
import cats.syntax.all.*
import org.jooq.{DSLContext, Record}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.jdk.OptionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.database.Tables.{CHALLENGE, CHALLENGE_LANGUAGE, LEETCODE_CHALLENGE}
import com.wenjunhuang.codeepiphany.leetcode.model.*
import com.wenjunhuang.codeepiphany.leetcode.model.runCode.LeetCodeRunResult
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.SubmissionResult.Processing
import com.wenjunhuang.codeepiphany.services.{console, BaseCodeEvaluationService, ChallengeRepository}
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.Tabulator

class LeetCodeEvaluationService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](
  project: Project,
  private val myLeetCode: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
) extends BaseCodeEvaluationService[F](project, myLeetCode) {
  override type EvaluationRequest  = LeetCodeEvaluationRequest
  override type EvaluationResponse = LeetCodeRunResult

  override protected def prepareRequest(
    item: ChallengeSettings.ChallengeSettingsStateItem,
    customTestCases: Option[String]
  ): F[LeetCodeEvaluationRequest] = {
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource[F]
      .use(client => Async[F].delay(queryChallengeInfo(item, client)))
  }

  private def queryChallengeInfo(item: ChallengeSettingsStateItem, client: DSLContext): LeetCodeEvaluationRequest = {
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

  private def parseLeetCodeRecord(record: Record): Option[LeetCodeEvaluationRequest] = {
    for {
      dojoId <- Option(record.get(CHALLENGE.DOJOID))
      testCase = Option(record.get(LEETCODE_CHALLENGE.TESTCASE))
      language <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      slug <- Option(record.get(CHALLENGE.SLUG))
    } yield LeetCodeEvaluationRequest(dojoId, testCase.getOrElse(""), language, langVer, slug)
  }

  override protected def callApi(
    request: LeetCodeEvaluationRequest,
    code: String,
    customTestCases: Option[String]
  ): fs2.Stream[F, LeetCodeRunResult] = LeetCodeApi[F](myLeetCode)
    .runAnswer(
      request.leetCodeQuestionId,
      request.questionSlug,
      customTestCases.getOrElse(request.testCase),
      request.language,
      request.languageVersion,
      code
    )

  override protected def handleEvaluationResponse(
    response: LeetCodeRunResult,
    request: EvaluationRequest,
    code: String,
    customTestCases: Option[String]
  ): F[EvaluationResponseInfo] = {
    Async[F].delay {
      response match
        case success: LeetCodeRunResult.Success =>
          val result = myLeetCode.fromLeetCodeRunResult(success.statusMsg, success.correctAnswer)
          val message = result match
            case SubmissionResult.Success if success.correctAnswer.contains(true) =>
              "🎉 Passed!"
            case SubmissionResult.Failure =>
              s"Wrong Answer!\n${formatResultDiff(success, customTestCases.getOrElse(request.testCase))}"
            case result =>
              formatErrorMessage(result, success)
          EvaluationResponseInfo(result, message)
        case _ =>
          EvaluationResponseInfo(Processing, "Evaluation is running...")
    }
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

  private def formatResultDiff(result: LeetCodeRunResult.Success, testCase: String): String = {
    val cases = StringUtil.splitByLines(testCase).toList
    val comparisons = cases.zip(result.codeAnswer.zip(result.expectedCodeAnswer)) ++
      cases.zip(result.stdOutputList.zip(result.expectedStdOutputList))

    Tabulator.format(
      (List("Case", "Your Answer", "Expected Answer") +:
        comparisons.collect {
          case (testCase, (output, expected)) if output != expected =>
            List(
              StringUtil.escapeLineBreak(testCase),
              StringUtil.escapeLineBreak(output),
              StringUtil.escapeLineBreak(expected)
            )
        })*
    )
  }
  override protected def reportEvaluationResult(
    lastResponseInfo: EvaluationResponseInfo,
    lastResponse: LeetCodeRunResult
  ): F[Unit] = {
    lastResponseInfo.result match
      case SubmissionResult.Success =>
        console.info[F](project, s"${SubmissionResult.Success.show}\n${lastResponseInfo.message}")
      case result =>
        console.error[F](project, s"${result.show}\n${lastResponseInfo.message}")
  }

  case class LeetCodeEvaluationRequest(
    leetCodeQuestionId: String,
    testCase: String,
    language: Language,
    languageVersion: LanguageVersion,
    questionSlug: String
  )
}
