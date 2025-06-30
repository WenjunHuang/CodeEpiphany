package com.wenjunhuang.codeepiphany.atcoder.services

import cats.effect.{Async, Concurrent, IO}
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderChallengeCodeTemplate
import com.wenjunhuang.codeepiphany.atcoder.settings.{AtCoderSettings, AtCoderSettingsConfigurable}
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.database.tables.records.{AtcoderProblemsRecord, ChallengeLanguageRecord, ChallengeRecord}
import com.wenjunhuang.codeepiphany.model.newtypes.CodeDojoChallengeId
import com.wenjunhuang.codeepiphany.model.{ChallengeDifficulty, CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings
import com.wenjunhuang.codeepiphany.utils.template.VelocityTool
import org.jooq.DSLContext
import org.typelevel.log4cats.LoggerFactory

class AtCoderOpenChallengeService(project: Project)
    extends BaseOpenChallengeService[AtcoderProblemsRecord, AtCoderChallengeCodeTemplate](
      project,
      CodeDojo.AtCoder,
      classOf[AtCoderSettingsConfigurable]
    ) {
  override protected def findLanguageSetting(
    language: Language,
    languageVersion: LanguageVersion
  ): Option[BaseCodeDojoSettings.LanguageSettingsState] =
    AtCoderSettings.getInstance(myProject).getLanguageSetting(language, languageVersion)

  override protected def fillChallengeRecord(record: ChallengeRecord, state: ServiceState): Unit = {
    record.setDescription(state.template.description)
    record.setDifficulty(
      ChallengeDifficulty
        .CodeDojoDefined(CodeDojo.AtCoder, Option(state.req.getDifficulty).map(_.toString).getOrElse(""))
        .value
    )
    record.setDojo(CodeDojo.AtCoder.value)
    record.setDojoid(state.req.getProblemid)
    record.setSlug(VelocityTool.slugify(state.req.getName))
    record.setTitle(state.req.getName)
  }

  override protected def storeExtraChallengeData(
    dsl: DSLContext,
    challenge: AtCoderChallengeCodeTemplate,
    challengeRecord: ChallengeRecord,
    challengeLanguageRecord: ChallengeLanguageRecord
  ): Unit = {
    val atcoderRecord =
      dsl.fetchOne(ATCODER_CHALLENGE, ATCODER_CHALLENGE.ID.eq(challengeRecord.getId)) match
        case null =>
          dsl.newRecord(ATCODER_CHALLENGE).setId(challengeRecord.getId)
        case r => r
    atcoderRecord.setDifficulty(challenge.record.getDifficulty)
    atcoderRecord.setContestid(challenge.record.getContestid)
    atcoderRecord.setProblemindex(challenge.record.getProblemindex)
    atcoderRecord.setTitle(challenge.record.getTitle)
    atcoderRecord.store()
  }

  override protected def createTemplate(
    req: AtcoderProblemsRecord,
    language: Language,
    languageVersion: LanguageVersion
  ): IO[(CodeDojoChallengeId, AtCoderChallengeCodeTemplate)] = {
    AtCoderApi
      .getChallengeData(req.getContestid, req.getProblemid)
      .flatMap(_.liftTo[IO](new Exception(s"Challenge ${req.getName} is not available")))
      .flatMap { content =>
        if content.supportedLanguages.contains((language, languageVersion)) then
          IO.pure(
            CodeDojoChallengeId(req.getProblemid),
            AtCoderChallengeCodeTemplate(
              contestId = req.getContestid,
              contestTitle = req.get(ATCODER_CONTESTS.TITLE),
              id = req.getProblemid,
              problemIndex = req.getProblemindex,
              name = req.getName,
              title = req.getTitle,
              description = content.description,
              codeDojo = CodeDojo.AtCoder,
              language = language,
              languageVersion = languageVersion,
              record = req,
              content = content,
              testCases = content.testCases
            )
          )
        else
          IO.raiseError(new Exception(s"This challenge does not support ${language.show} ${languageVersion.version}"))
      }
  }
}
