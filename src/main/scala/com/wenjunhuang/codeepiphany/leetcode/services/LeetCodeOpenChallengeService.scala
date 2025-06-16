package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.{ Concurrent, IO }
import cats.syntax.all.*

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.database.tables.records.{ ChallengeLanguageRecord, ChallengeRecord }
import com.wenjunhuang.codeepiphany.leetcode.models.*
import com.wenjunhuang.codeepiphany.leetcode.settings.{
  LeetCodeCNSettings,
  LeetCodeCNSettingsConfigurable,
  LeetCodeSettings,
  LeetCodeSettingsConfigurable
}
import com.wenjunhuang.codeepiphany.model.newtypes.CodeDojoChallengeId
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings
import org.jooq.DSLContext
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.util.text.StringUtil
import scala.jdk.CollectionConverters.*

import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

case class LeetCodeOpenChallengeRequest(questionSlug: String)

class LeetCodeOpenChallengeService(
  project: Project,
  private val myLeetCodeDojo: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
) extends BaseOpenChallengeService[LeetCodeOpenChallengeRequest, LeetCodeChallengeCodeTemplate](
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
  ): IO[(CodeDojoChallengeId, LeetCodeChallengeCodeTemplate)] = {
    LeetCodeApi(myLeetCodeDojo)
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
          IO.raiseError(new Exception(s"This challenge does not support ${language.show}${languageVersion.version}"))
        case Some((content, codeSnippet)) =>
          IO.pure(
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
                content = content,
                testCases = StringUtil
                  .splitByLines(content.exampleTestcases)
                  .toList
                  .grouped(2)
                  .toList
                  .collect { case input +: expectedOutput +: Nil =>
                    ChallengeSettings.TestCase(input = input, expectedOutput = expectedOutput)
                  }
              )
            )
          )
      }
  }
}
