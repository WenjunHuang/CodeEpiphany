package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.IO

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
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCodeCN
import com.wenjunhuang.codeepiphany.model.newtypes.CodeDojoChallengeId
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings
import org.jooq.DSLContext
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import scala.jdk.CollectionConverters.*

import com.wenjunhuang.codeepiphany.PluginBundle

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
      .setTestcase(challenge.content.exampleTestcaseList.mkString("\n"))
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
          IO.raiseError(
            new Exception(PluginBundle.message("error.notSupportLanguage", language.show, languageVersion.version))
          )
        case Some((content, codeSnippet)) =>
          IO.delay {
            val inputs = content.exampleTestcaseList
            val expectedOutputs =
              if (myCodeDojo == LeetCodeCN && content.content.contains("English description is not available for the problem")) {
                val document       = Jsoup.parse(content.translatedContent.getOrElse(""))
                val outputElements = document.select("pre strong:containsOwn(输出)")
                if (outputElements.isEmpty) {
                  document
                    .select("p strong:containsOwn(输出) + span.example-io")
                    .asScala
                    .map { el =>
                      el.text()
                    }
                    .toList match {
                    case Nil =>
                      document
                        .select("blockquote p:containsOwn(输出) code")
                        .asScala
                        .map { el =>
                          el.text().trim
                        }
                        .toList
                    case outputs => outputs
                  }
                } else {
                  outputElements.asScala.map { el =>
                    el.nextSibling() match {
                      case textNode: TextNode => textNode.text().trim
                      case _                  => el.nextSibling().toString
                    }
                  }.toList
                }
              } else {
                val document       = Jsoup.parse(content.content)
                val outputElements = document.select("pre strong:containsOwn(Output)")
                if (outputElements.isEmpty) {
                  document
                    .select("p strong:containsOwn(Output) + span.example-io")
                    .asScala
                    .map { el =>
                      el.text()
                    }
                    .toList
                } else {
                  outputElements.asScala.map { el =>
                    el.nextSibling() match {
                      case textNode: TextNode => textNode.text().trim
                      case _                  => el.nextSibling().toString
                    }
                  }.toList
                }
              }

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
                testCases = inputs
                  .zip(expectedOutputs)
                  .map { case (input, expectedOutput) =>
                    ChallengeSettings.TestCase(input, expectedOutput)
                  }
              )
            )
          }
      }
  }
}
