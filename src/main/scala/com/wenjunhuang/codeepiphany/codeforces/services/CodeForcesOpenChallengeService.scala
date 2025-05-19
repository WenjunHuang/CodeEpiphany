package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.{ Async, Concurrent }
import cats.syntax.all.*
import org.jooq.DSLContext
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.codeforces.models.{ codeForcesRatingToDifficulty, CodeForcesChallengeCodeTemplate }
import com.wenjunhuang.codeepiphany.codeforces.settings.{ CodeForcesSettings, CodeForcesSettingsConfigurable }
import com.wenjunhuang.codeepiphany.database.tables.records.{
  ChallengeLanguageRecord,
  ChallengeRecord,
  CodeforcesProblemsetsRecord
}
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.{ ApiError, CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.model.newtypes.CodeDojoChallengeId
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings
import com.wenjunhuang.codeepiphany.utils.template.VelocityTool

class CodeForcesOpenChallengeService[F[_]: { Async, Concurrent, HttpClientManager, LoggerFactory }](project: Project)
    extends BaseOpenChallengeService[F, CodeforcesProblemsetsRecord, CodeForcesChallengeCodeTemplate](
      project,
      CodeDojo.CodeForces,
      classOf[CodeForcesSettingsConfigurable]
    ) {
  override protected def findLanguageSetting(
    language: Language,
    languageVersion: LanguageVersion
  ): Option[BaseCodeDojoSettings.LanguageSettingsState] =
    CodeForcesSettings.getInstance(myProject).getLanguageSetting(language, languageVersion)

  override protected def fillChallengeRecord(record: ChallengeRecord, state: ServiceState): Unit = {
    record.setDescription(state.template.content.description)
    record.setDifficulty(codeForcesRatingToDifficulty(Option(state.req.getRating).map(_.toInt)).value)
    record.setDojo(CodeDojo.CodeForces.value)
    record.setDojoid(state.req.getContestidindex)
    record.setSlug(VelocityTool.slugify(state.req.getName))
    record.setTitle(state.req.getName)
  }

  override protected def storeExtraChallengeData(
    dsl: DSLContext,
    challenge: CodeForcesChallengeCodeTemplate,
    challengeRecord: ChallengeRecord,
    challengeLanguageRecord: ChallengeLanguageRecord
  ): Unit = {
    val codeForcesChallenge =
      dsl.fetchOne(CODEFORCES_CHALLENGE, CODEFORCES_CHALLENGE.ID.eq(challengeRecord.getId)) match
        case null =>
          dsl.newRecord(CODEFORCES_CHALLENGE).setId(challengeRecord.getId)
        case r => r
    codeForcesChallenge.setContestid(challenge.content.contestId)
    codeForcesChallenge.setIndex(challenge.content.index)
    codeForcesChallenge.setProblemsetname(challenge.problemsetName.orNull)
    codeForcesChallenge.store()
  }

  override protected def createTemplate(
    req: CodeforcesProblemsetsRecord,
    language: Language,
    languageVersion: LanguageVersion
  ): F[(CodeDojoChallengeId, CodeForcesChallengeCodeTemplate)] = {
    CodeForcesApi[F]
      .getChallengeData(Option(req.getProblemsetname), req.getContestid, req.getIndex)
      .flatMap(_.liftTo[F](ApiError.NotFound(CodeDojo.CodeForces, s"Challenge ${req.getName} is not available")))
      .flatMap { content =>
        if content.supportedLanguages.contains((language, languageVersion)) then
          Async[F].pure(
            (
              CodeDojoChallengeId(req.getContestidindex),
              CodeForcesChallengeCodeTemplate(
                contestIdIndex = s"${Option(req.getContestid).map(_.toString).getOrElse("")}${req.getIndex}",
                name = req.getName,
                language = language,
                languageVersion = languageVersion,
                rating = Option(req.getRating).map(_.toInt),
                problemsetName = Option(req.getProblemsetname),
                tags = req.getTags.split(",").toList,
                content = content
              )
            )
          )
        else
          Async[F].raiseError(
            ApiError.InvalidContent(
              CodeDojo.CodeForces,
              s"This challenge does not support ${language.show}${languageVersion.version}"
            )
          )
      }
  }
}
