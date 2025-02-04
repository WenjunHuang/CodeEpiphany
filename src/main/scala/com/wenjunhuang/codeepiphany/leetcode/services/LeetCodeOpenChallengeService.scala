package com.wenjunhuang.codeepiphany.leetcode.services

import cats.syntax.all.*
import cats.effect.{ Async, Concurrent }
import org.jooq.DSLContext
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.database.Tables.*

import com.wenjunhuang.codeepiphany.database.tables.records.{ ChallengeLanguageRecord, ChallengeRecord }
import com.wenjunhuang.codeepiphany.leetcode.model.LeetCodeChallengeCodeTemplate
import com.wenjunhuang.codeepiphany.leetcode.settings.{
  LeetCodeCNSettings,
  LeetCodeCNSettingsConfigurable,
  LeetCodeSettings,
  LeetCodeSettingsConfigurable
}
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.model.newtypes.CodeDojoChallengeId
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings
import com.wenjunhuang.codeepiphany.leetcode.model.*

case class LeetCodeOpenChallengeRequest(questionSlug: String)
class LeetCodeOpenChallengeService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](
  project: Project,
  private val myLeetCodeDojo: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
) extends BaseOpenChallengeService[F, LeetCodeOpenChallengeRequest, LeetCodeChallengeCodeTemplate](
      project,
      myLeetCodeDojo,
      if myLeetCodeDojo == CodeDojo.LeetCode then classOf[LeetCodeSettingsConfigurable]
      else classOf[LeetCodeCNSettingsConfigurable]
    ) {
  override protected def findLanguageSetting(
    language: Language,
    languageVersion: LanguageVersion
  ): Option[BaseCodeDojoSettings.LanguageSettingsState] =
    myLeetCodeDojo match
      case CodeDojo.LeetCodeCN =>
        LeetCodeCNSettings.getInstance(myProject).getLanguageSetting(language, languageVersion)
      case CodeDojo.LeetCode =>
        LeetCodeSettings.getInstance(myProject).getLanguageSetting(language, languageVersion)

  override protected def fillChallengeRecord(record: ChallengeRecord, state: ServiceState): Unit = {
    val template = state.template
    record.setDescription(template.description)
    record.setDifficulty(template.difficulty)
    record.setDojo(template.dojo.value)
    record.setDojoid(template.questionId)
    record.setSlug(template.slug)
    record.setTitle(template.name)
  }

  override protected def storeExtraChallengeData(
    dsl: DSLContext,
    challenge: LeetCodeChallengeCodeTemplate,
    challengeRecord: ChallengeRecord,
    challengeLanguageRecord: ChallengeLanguageRecord
  ): Unit = {
    val leetCodeChallengeRecord = dsl
      .fetchOne(LEETCODE_CHALLENGE, LEETCODE_CHALLENGE.ID.eq(challengeRecord.getId)) match
      case null =>
        dsl.newRecord(LEETCODE_CHALLENGE).setId(challengeRecord.getId)
      case r => r

    leetCodeChallengeRecord
      .setFrontendquestionid(challenge.frontendQuestionId)
      .setTestcase(challenge.content.exampleTestcases)
    leetCodeChallengeRecord.store()
  }

  override protected def createTemplate(
    req: LeetCodeOpenChallengeRequest,
    language: Language,
    languageVersion: LanguageVersion
  ): F[(CodeDojoChallengeId, LeetCodeChallengeCodeTemplate)] = {
    LeetCodeApi[F](myLeetCodeDojo)
      .getQuestionData(req.questionSlug)
      .map { content =>
        content.codeSnippets.find { snippet =>
          myLeetCodeDojo
            .fromLeetCodeLanguage(snippet.langSlug)
            .contains(language, languageVersion)
        }.map { codeSnippet => (content, codeSnippet) }
      }
      .flatMap {
        case None =>
          Async[F].raiseError(new Exception(s"This challenge does not support ${language.show}${languageVersion.version}"))
        case Some((content, codeSnippet)) =>
          Async[F].pure(
            (
              CodeDojoChallengeId(content.questionId),
              LeetCodeChallengeCodeTemplate(
                questionId = content.questionId,
                frontendQuestionId = content.frontendQuestionId,
                dojo = myLeetCodeDojo,
                name = content.translatedTitle.filter(_.nonEmpty).getOrElse(content.title),
                code = codeSnippet.code,
                slug = content.titleSlug,
                description = content.translatedContent.filter(_.nonEmpty).getOrElse(content.content),
                difficulty = myLeetCodeDojo.fromLeetCodeDifficulty(content.difficulty).value,
                language = language,
                languageVersion = languageVersion,
                content = content
              )
            )
          )
      }
  }
}
