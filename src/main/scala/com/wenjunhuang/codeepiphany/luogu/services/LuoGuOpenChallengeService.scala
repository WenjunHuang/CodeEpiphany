package com.wenjunhuang.codeepiphany.luogu.services

import cats.syntax.all.*
import cats.effect.{ Async, Concurrent }
import org.jooq.DSLContext
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.database.tables.records.{ ChallengeLanguageRecord, ChallengeRecord }
import com.wenjunhuang.codeepiphany.luogu.models.{ LuoGuChallengeCodeTemplate, LuoGuChallengeItem }
import com.wenjunhuang.codeepiphany.luogu.settings.{ LuoGuSettings, LuoGuSettingsConfigurable }
import com.wenjunhuang.codeepiphany.model.{ ChallengeDifficulty, CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.model.newtypes.CodeDojoChallengeId
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings
import com.wenjunhuang.codeepiphany.utils.template.VelocityTool

class LuoGuOpenChallengeService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](project: Project)
    extends BaseOpenChallengeService[F, LuoGuChallengeItem, LuoGuChallengeCodeTemplate](
      project,
      CodeDojo.LuoGu,
      classOf[LuoGuSettingsConfigurable]
    ) {
  override protected def findLanguageSetting(
    language: Language,
    languageVersion: LanguageVersion
  ): Option[BaseCodeDojoSettings.LanguageSettingsState] = {
    LuoGuSettings.getInstance(myProject).getLanguageSetting(language, languageVersion)
  }

  override protected def storeExtraChallengeData(
    dsl: DSLContext,
    challenge: LuoGuChallengeCodeTemplate,
    challengeRecord: ChallengeRecord,
    challengeLanguageRecord: ChallengeLanguageRecord
  ): Unit = {}

  override protected def fillChallengeRecord(record: ChallengeRecord, state: ServiceState): Unit = {
    record.setDescription(state.template.description)
    record.setDifficulty(
      ChallengeDifficulty
        .CodeDojoDefined(CodeDojo.LuoGu, state.req.difficulty.value.toString)
        .value
    )
    record.setDojo(CodeDojo.LuoGu.value)
    record.setDojoid(state.req.pid)
    record.setSlug(VelocityTool.slugify(state.req.title))
    record.setTitle(state.req.title)
  }

  override protected def createTemplate(
    req: LuoGuChallengeItem,
    language: Language,
    languageVersion: LanguageVersion
  ): F[(CodeDojoChallengeId, LuoGuChallengeCodeTemplate)] = {
    LuoGuApi[F]()
      .getChallengeData(req.pid)
      .flatMap { content =>
        if content.supportedLanguages.contains((language, languageVersion)) then
          Async[F].pure(
            CodeDojoChallengeId(req.pid),
            LuoGuChallengeCodeTemplate(req.pid, content.title, language, languageVersion, content.description)
          )
        else
          Async[F].raiseError(
            new Exception(s"This challenge does not support ${language.show} ${languageVersion.version}")
          )
      }
  }
}
