package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.database.Tables.{ CHALLENGE, CHALLENGE_LANGUAGE, HACKERRANK_CHALLENGE }
import com.wenjunhuang.codeepiphany.hackerrank.models.{ HackerRankContest, HackerRankRunCodeResponse }
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.{ console, BaseCodeEvaluationService, ChallengeRepository }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.{ ChallengeSettingsStateItem, TestCase }
import fs2.Stream
import org.jooq.{ DSLContext, Record }
import org.typelevel.ci.CIString

import scala.jdk.OptionConverters.*

class HackerRankEvaluationService(project: Project) extends BaseCodeEvaluationService(project, HackerRank) {
  override type EvaluationRequest  = HREvaluationRequest
  override type EvaluationResponse = HackerRankRunCodeResponse

  override protected def prepareRequest(
    item: ChallengeSettingsStateItem,
    customTestCases: Option[List[TestCase]]
  ): IO[HREvaluationRequest] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource
      .use(client => IO.delay(queryChallengeBasicInfo(item, client)))

  override protected def callApi(
    request: HREvaluationRequest,
    code: String,
    customTestCases: Option[List[TestCase]]
  ): Stream[IO, HackerRankRunCodeResponse] =
    HackerRankApi.runAnswer(request.slug, request.contest, request.language, request.languageVersion, code)

  override protected def handleEvaluationResponse(
    response: HackerRankRunCodeResponse,
    request: HREvaluationRequest,
    code: String,
    customTestCases: Option[List[TestCase]]
  ): IO[EvaluationResponseInfo] =
    IO.delay {
      if response.status == 0 then EvaluationResponseInfo(SubmissionResult.Processing, "")
      else
        response.compilemessage.filter(_.nonEmpty) match
          case Some(message) =>
            EvaluationResponseInfo(SubmissionResult.CompilationError, message)
          case None =>
            if response.testcaseStatus.contains(0) then EvaluationResponseInfo(SubmissionResult.Failure, "Wrong Answer")
            else EvaluationResponseInfo(SubmissionResult.Success, "")
    }

  override protected def reportEvaluationResult(
    lastResponseInfo: EvaluationResponseInfo,
    lastResponse: HackerRankRunCodeResponse
  ): IO[Unit] = {
    lastResponseInfo.result match
      case SubmissionResult.Success =>
        console.info(myProject, myCodeDojo, "🎉 Passed!")
      case _ =>
        console.error(myProject, myCodeDojo, s"${lastResponseInfo.result.show}: ${lastResponseInfo.message}")
  }

  private def queryChallengeBasicInfo(item: ChallengeSettingsStateItem, client: DSLContext): HREvaluationRequest = {
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

  private def parseHackerRankRecord(record: Record): Option[HREvaluationRequest] = {
    for {
      challengeSlug <- Option(record.get(CHALLENGE.SLUG))
      contestSlug <- Option(record.get(HACKERRANK_CHALLENGE.CONTESTSLUG))
        .flatMap(it => HackerRankContest.fromCIString(CIString(it)))
      language <- Option(record.get(CHALLENGE_LANGUAGE.LANGUAGE))
        .flatMap(it => Language.fromCIString(CIString(it)))
      langVer <- Option(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
    } yield HREvaluationRequest(contestSlug, language, LanguageVersion.fromString(langVer), challengeSlug)
  }

  case class HREvaluationRequest(
    contest: HackerRankContest,
    language: Language,
    languageVersion: LanguageVersion,
    slug: String
  )
}
