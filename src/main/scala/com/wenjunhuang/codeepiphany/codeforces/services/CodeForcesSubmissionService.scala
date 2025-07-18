package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.IO
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesSubmissionResponse
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettingsConfigurable
import com.wenjunhuang.codeepiphany.database.Tables.{CHALLENGE, CHALLENGE_LANGUAGE, CODEFORCES_CHALLENGE}
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion, SubmissionResult}
import com.wenjunhuang.codeepiphany.services.{BaseSubmissionService, ChallengeRepository, console}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import org.jooq.{DSLContext, Record}
import org.typelevel.ci.CIString

import scala.jdk.OptionConverters.*

class CodeForcesSubmissionService(project: Project) extends BaseSubmissionService(project, CodeForces) {
  override type SubmissionRequest  = CFRequest
  override type SubmissionResponse = CodeForcesSubmissionResponse

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): IO[CFRequest] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource
      .use { client => IO.delay(createCFRequest(item, client)) }

  override protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: CodeForcesSubmissionResponse
  ): SubmissionResponseInfo = SubmissionResponseInfo(response.result, response.message, response.submissionId.toString)

  override protected def callApi(
    basicInfo: CFRequest,
    processedCode: String
  ): fs2.Stream[IO, CodeForcesSubmissionResponse] =
    CodeForcesApi.submitAnswer(
      basicInfo.contestId,
      basicInfo.index,
      basicInfo.problemsetName,
      basicInfo.programTypeId,
      processedCode
    )

  override protected def reportSubmitResult(
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: CodeForcesSubmissionResponse
  ): IO[Unit] = {
    lastResponseInfo.result match
      case SubmissionResult.Success =>
        console.info(project, PluginBundle.message("submission.passed") + "\n${lastResponse.message}")
      case _ =>
        console.error(project, s"${lastResponseInfo.result.show}\n${lastResponse.message}")
  }

  private def createCFRequest(item: ChallengeSettingsStateItem, client: DSLContext): CFRequest = {
    client
      .select(
        CHALLENGE.DOJOID,
        CODEFORCES_CHALLENGE.CONTESTID,
        CODEFORCES_CHALLENGE.INDEX,
        CODEFORCES_CHALLENGE.PROBLEMSETNAME,
        CHALLENGE_LANGUAGE.LANGUAGE,
        CHALLENGE_LANGUAGE.LANGUAGEVERSION
      )
      .from(CHALLENGE)
      .innerJoin(CODEFORCES_CHALLENGE)
      .on(CHALLENGE.ID.eq(CODEFORCES_CHALLENGE.ID))
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
      .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
      .fetchOptional()
      .toScala
      .flatMap(parseCodeForcesRecord)
      .getOrElse(throw new IllegalStateException("Cannot find challenge data"))
  }

  private def parseCodeForcesRecord(record: Record): Option[CFRequest] = {
    for {
      contestId <- Option(record.get(CODEFORCES_CHALLENGE.CONTESTID)).map(_.toLong)
      index     <- Option(record.get(CODEFORCES_CHALLENGE.INDEX))
      problemset = Option(record.get(CODEFORCES_CHALLENGE.PROBLEMSETNAME))
      language <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      programTypeId <- resolveProgramType(language, langVer)
    } yield CFRequest(contestId, index, problemset, language, langVer, programTypeId)
  }
  private def resolveProgramType(language: Language, version: LanguageVersion): Option[String] =
    CodeForcesSettingsConfigurable.CODEFORCES_LANGUAGES.get((language, version))

  case class CFRequest(
    contestId: Long,
    index: String,
    problemsetName: Option[String],
    language: Language,
    langVer: LanguageVersion,
    programTypeId: String
  )
}
