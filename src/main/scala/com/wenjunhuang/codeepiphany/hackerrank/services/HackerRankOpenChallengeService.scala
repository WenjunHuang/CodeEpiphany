package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.{ Async, Concurrent }
import cats.syntax.all.*
import org.jooq.DSLContext
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.database.tables.records.{ ChallengeLanguageRecord, ChallengeRecord }
import com.wenjunhuang.codeepiphany.hackerrank.models.{
  HackerRankChallengeCodeTemplate,
  HackerRankChallengeContent,
  HackerRankContest,
  HackerRankLanguageTemplate
}
import com.wenjunhuang.codeepiphany.hackerrank.settings.{ HackerRankSettings, HackerRankSettingsConfigurable }
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.newtypes.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings.LanguageSettingsState
import com.wenjunhuang.codeepiphany.utils.implicits.*

case class HackerRankOpenChallengeRequest(challengeSlug: String, contest: HackerRankContest)
class HackerRankOpenChallengeService[F[_]: { Async, Concurrent, HttpClientManager, LoggerFactory }](project: Project)
    extends BaseOpenChallengeService[F, HackerRankOpenChallengeRequest, HackerRankChallengeCodeTemplate](
      project,
      CodeDojo.HackerRank,
      classOf[HackerRankSettingsConfigurable]
    ) {
  override protected def findLanguageSetting(
    language: Language,
    languageVersion: LanguageVersion
  ): Option[LanguageSettingsState] =
    HackerRankSettings.getInstance(myProject).getLanguageSetting(language, languageVersion)

  override protected def fillChallengeRecord(record: ChallengeRecord, state: ServiceState): Unit = {
    record.setDescription(state.template.description)
    record.setDifficulty(state.template.difficulty)
    record.setDojo(CodeDojo.HackerRank.value)
    record.setDojoid(state.template.id)
    record.setSlug(state.template.slug)
    record.setTitle(state.template.name)
  }

  override protected def storeExtraChallengeData(
    dsl: DSLContext,
    challenge: HackerRankChallengeCodeTemplate,
    challengeRecord: ChallengeRecord,
    challengeLanguageRecord: ChallengeLanguageRecord
  ): Unit = {
    val hackerRankRecord =
      (dsl.fetchOne(HACKERRANK_CHALLENGE, HACKERRANK_CHALLENGE.ID.eq(challengeRecord.getId)) match
        case null =>
          dsl.newRecord(HACKERRANK_CHALLENGE).setId(challengeRecord.getId)
        case r => r
      ).setContest(challenge.contest)
        .setContestslug(challenge.contest)
    hackerRankRecord.store()

    val hackerRankLangRecord = dsl.fetchOne(
      HACKERRANK_CHALLENGE_LANGUAGE,
      HACKERRANK_CHALLENGE_LANGUAGE.ID.eq(challengeLanguageRecord.getId)
    ) match
      case null => dsl.newRecord(HACKERRANK_CHALLENGE_LANGUAGE).setId(challengeLanguageRecord.getId)
      case r    => r
    hackerRankLangRecord.setCodeheader(StringUtil.convertLineSeparators(challenge.header))
    hackerRankLangRecord.setCodetemplate(StringUtil.convertLineSeparators(challenge.template))
    hackerRankLangRecord.setCodetail(StringUtil.convertLineSeparators(challenge.tail))
    hackerRankLangRecord.store()
  }

  override protected def createTemplate(
    req: HackerRankOpenChallengeRequest,
    language: Language,
    languageVersion: LanguageVersion
  ): F[(CodeDojoChallengeId, HackerRankChallengeCodeTemplate)] = {
    HackerRankApi[F]
      .getChallengeContent(req.challengeSlug, req.contest)
      .flatMap { content =>
        content.codeTemplates.get((language, languageVersion)) match
          case Some(temp) =>
            Async[F].delay {
              val template = handleValidTemplate(content, temp, req.contest, language, languageVersion)
              (CodeDojoChallengeId(template.id), template)
            }
          case None =>
            Async[F].raiseError(
              new Exception(s"This challenge does not support ${language.show}${languageVersion.version}")
            )
      }
  }

  private def handleValidTemplate(
    content: HackerRankChallengeContent,
    temp: HackerRankLanguageTemplate,
    contest: HackerRankContest,
    language: Language,
    languageVersion: LanguageVersion
  ) = {
    HackerRankChallengeCodeTemplate(
      content.detail.id.toString,
      HackerRank,
      content.detail.name,
      content.detail.slug,
      content.detail.bodyHtml.getOrElse(""),
      temp.header,
      temp.template,
      temp.tail,
      contest.slug,
      ChallengeDifficulty.fromCIString(CIString(content.detail.difficultyName)).get.value,
      language,
      languageVersion
    )
  }
}
