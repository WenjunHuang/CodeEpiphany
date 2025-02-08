package com.wenjunhuang.codeepiphany.atcoder.services

import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.syntax.all.*
import fs2.Stream
import org.jooq.{DSLContext, Record}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.jdk.OptionConverters.*

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderSubmissionResponse
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettingsConfigurable
import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesSubmissionResponse
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettingsConfigurable
import com.wenjunhuang.codeepiphany.database.Tables.{ATCODER_CHALLENGE, CHALLENGE, CHALLENGE_LANGUAGE, CODEFORCES_CHALLENGE}
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion, SubmissionResult}
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.services.{console, BaseSubmissionService, ChallengeRepository}
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem

class AtCoderSubmissionService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](project: Project)
    extends BaseSubmissionService[F](project, CodeForces) {
  override type SubmissionRequest  = Request
  override type SubmissionResponse = AtCoderSubmissionResponse

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): F[Request] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource[F]
      .use { client => Async[F].delay(createRequest(item, client)) }

  override protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: AtCoderSubmissionResponse
  ): SubmissionResponseInfo = SubmissionResponseInfo(response.result, response.message, response.submissionId.toString)

  override protected def callApi(basicInfo: Request, processedCode: String): Stream[F, AtCoderSubmissionResponse] =
    AtCoderApi[F]().submitAnswer(basicInfo.problemId, basicInfo.languageId, processedCode)

  override protected def reportSubmitResult(
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: AtCoderSubmissionResponse
  ): F[Unit] = {
    lastResponseInfo.result match
      case SubmissionResult.Success =>
        console.info[F](project, s"🎉 Passed!\n${lastResponse.message}")
      case _ =>
        console.error[F](project, s"${lastResponseInfo.result.show}\n${lastResponse.message}")
  }

  private def createRequest(item: ChallengeSettingsStateItem, client: DSLContext): Request = {
    client
      .select(CHALLENGE.DOJOID, CHALLENGE_LANGUAGE.LANGUAGE, CHALLENGE_LANGUAGE.LANGUAGEVERSION)
      .from(CHALLENGE)
      .innerJoin(CODEFORCES_CHALLENGE)
      .on(CHALLENGE.ID.eq(CODEFORCES_CHALLENGE.ID))
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
      .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
      .fetchOptional()
      .toScala
      .flatMap(parseRecord)
      .getOrElse(throw new IllegalStateException("Cannot find challenge data"))
  }

  private def parseRecord(record: Record): Option[Request] = {
    for {
      problemId <- Option(record.get(CHALLENGE.DOJOID))
      language  <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      programTypeId <- resolveLanguageId(language, langVer)
    } yield Request(problemId, language, langVer, programTypeId)
  }
  private def resolveLanguageId(language: Language, version: LanguageVersion): Option[String] =
    AtCoderSettingsConfigurable.ATCODER_LANGUAGES.get((language, version))

  case class Request(problemId: String, language: Language, langVer: LanguageVersion, languageId: String)
}
