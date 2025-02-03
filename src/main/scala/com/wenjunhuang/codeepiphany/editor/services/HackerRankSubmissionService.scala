package com.wenjunhuang.codeepiphany.editor.services

import cats.syntax.all.*
import cats.effect.Concurrent
import cats.effect.kernel.Async
import org.jooq.{ DSLContext, Record }
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.hackerrank.model.{ HackerRankContest, HackerRankSubmissionResponse }
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, CodeDojo, Language, LanguageVersion, SubmissionResult }
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.SubmissionId
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.utils.IdGenerator
import fs2.Stream
import scala.jdk.OptionConverters.*

import com.wenjunhuang.codeepiphany.model.SubmissionResult.Success
import com.wenjunhuang.codeepiphany.services.console

class HackerRankSubmissionService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](project: Project)
    extends BaseSubmissionService[F](project, HackerRank) {
  override type SubmissionRequest  = HRSubmissionRequest
  override type SubmissionResponse = HackerRankSubmissionResponse

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): F[HRSubmissionRequest] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource[F]
      .use { client => Async[F].delay(queryChallengeBasicInfo(item, client)) }

  override protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: HackerRankSubmissionResponse
  ): SubmissionResponseInfo = {
    val result = toSubmissionResult(response.status)
    val message =
      if result == SubmissionResult.CompilationError then response.compileMessage.getOrElse(response.status)
      else response.status
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
          .setSubmissionid(submissionId.value)
          .setTestcasemessage(message)
          .setNum(index)
          .setTestcasestatus(status)
          .setCodecheckersignal(signal)
          .setCodecheckertime(time.bigDecimal.floatValue())
        testcaseRecord.store()
      }
    SubmissionResponseInfo(result, message, response.id.toString)
  }

  override protected def callApi(
    basicInfo: HRSubmissionRequest,
    processedCode: String
  ): Stream[F, HackerRankSubmissionResponse] = HackerRankApi[F]().submitAnswer(
    basicInfo.slug,
    basicInfo.contest,
    basicInfo.language,
    basicInfo.languageVersion,
    processedCode
  )

  override protected def reportSubmitResult(
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: HackerRankSubmissionResponse
  ): F[Unit] = {
    lastResponseInfo.result match
      case Success =>
        console.info[F](project, "🎉 Passed!")
      case _ =>
        console.error[F](project, s"${lastResponseInfo.result.show}\n${lastResponseInfo.message}")
  }

  private def toSubmissionResult(status: String): SubmissionResult = {
    val ci = CIString(status)
    if ci.contains(CIString("Accepted")) then SubmissionResult.Success
    else if ci.contains(CIString("Wrong Answer")) then SubmissionResult.Failure
    else if ci.contains(CIString("Compilation error")) then SubmissionResult.CompilationError
    else if ci.contains(CIString("Terminated due to timeout")) then SubmissionResult.Timeout
    else if ci.contains(CIString("Processing")) then SubmissionResult.Processing
    else if ci.contains(CIString("Runtime Error")) then SubmissionResult.RuntimeError
    else SubmissionResult.Unknown
  }
  private def queryChallengeBasicInfo(item: ChallengeSettingsStateItem, client: DSLContext): HRSubmissionRequest = {
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

  private def parseHackerRankRecord(record: Record): Option[HRSubmissionRequest] = {
    for {
      challengeSlug <- Option(record.get(CHALLENGE.SLUG))
      contestSlug <- Option(record.get(HACKERRANK_CHALLENGE.CONTESTSLUG))
        .flatMap(it => HackerRankContest.fromCIString(CIString(it)))
      language <- Option(record.get(CHALLENGE_LANGUAGE.LANGUAGE))
        .flatMap(it => Language.fromCIString(CIString(it)))
      langVer <- Option(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
    } yield HRSubmissionRequest(contestSlug, language, LanguageVersion.fromString(langVer), challengeSlug)
  }
  case class HRSubmissionRequest(
    contest: HackerRankContest,
    language: Language,
    languageVersion: LanguageVersion,
    slug: String
  )
}
