package com.wenjunhuang.codeepiphany.atcoder.services

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderSubmissionResponse
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettingsConfigurable
import com.wenjunhuang.codeepiphany.database.Tables.{ ATCODER_CHALLENGE, CHALLENGE, CHALLENGE_LANGUAGE }
import com.wenjunhuang.codeepiphany.model.CodeDojo.AtCoder
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.model.{ Language, LanguageVersion, SubmissionResult }
import com.wenjunhuang.codeepiphany.services.{ console, BaseSubmissionService, ChallengeRepository }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import fs2.Stream
import org.jooq.{ DSLContext, Record }
import org.typelevel.ci.CIString

import scala.jdk.OptionConverters.*

class AtCoderSubmissionService(project: Project) extends BaseSubmissionService(project, AtCoder) {
  override type SubmissionRequest  = Request
  override type SubmissionResponse = AtCoderSubmissionResponse

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): IO[Request] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource
      .use { client => IO.delay(createRequest(item, client)) }

  override protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: AtCoderSubmissionResponse
  ): SubmissionResponseInfo = SubmissionResponseInfo(response.result, response.message, response.submissionId)

  override protected def callApi(basicInfo: Request, processedCode: String): Stream[IO, AtCoderSubmissionResponse] =
    AtCoderApi.submitAnswer(basicInfo.contestId, basicInfo.problemId, basicInfo.languageId, processedCode)

  override protected def reportSubmitResult(
    basicInfo: SubmissionRequest,
    submissionId: SubmissionId,
    processedCode: String,
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: AtCoderSubmissionResponse
  ): IO[Unit] = {
    lastResponseInfo.result match
      case SubmissionResult.Success =>
        console.info(project, PluginBundle.message("submission.passed"))
      case _ =>
        console.error(project, s"${lastResponseInfo.result.show}\n${lastResponseInfo.message}")
  }

  private def createRequest(item: ChallengeSettingsStateItem, client: DSLContext): Request = {
    client
      .select(
        CHALLENGE.DOJOID,
        CHALLENGE_LANGUAGE.LANGUAGE,
        CHALLENGE_LANGUAGE.LANGUAGEVERSION,
        ATCODER_CHALLENGE.CONTESTID
      )
      .from(CHALLENGE)
      .innerJoin(ATCODER_CHALLENGE)
      .on(CHALLENGE.ID.eq(ATCODER_CHALLENGE.ID))
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
      contestId <- Option(record.get(ATCODER_CHALLENGE.CONTESTID))
      problemId <- Option(record.get(CHALLENGE.DOJOID))
      language  <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      programTypeId <- resolveLanguageId(language, langVer)
    } yield Request(contestId, problemId, language, langVer, programTypeId)
  }
  private def resolveLanguageId(language: Language, version: LanguageVersion): Option[String] =
    AtCoderSettingsConfigurable.ATCODER_LANGUAGES.get((language, version))

  case class Request(
    contestId: String,
    problemId: String,
    language: Language,
    langVer: LanguageVersion,
    languageId: String
  )
}
