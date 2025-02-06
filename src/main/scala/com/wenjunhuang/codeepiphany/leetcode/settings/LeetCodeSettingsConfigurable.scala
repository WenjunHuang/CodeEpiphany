package com.wenjunhuang.codeepiphany.leetcode.settings

import io.circe.optics.JsonPath
import io.circe.parser.*
import java.nio.charset.StandardCharsets
import java.util.Objects
import org.apache.commons.io.IOUtils

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.leetcode.model.{LeetCodeChallengeCodeTemplate, LeetCodeChallengeData}
import com.wenjunhuang.codeepiphany.leetcode.settings.LeetCodeSettingsConfigurable.{getDemoTemplate, LANGUAGES}
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCode
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.settings.dojo.{BaseCodeDojoSettings, BaseSettingsConfigurable}

class LeetCodeSettingsConfigurable(project: Project)
    extends BaseSettingsConfigurable(
      project,
      CodeDojo.LeetCode,
      "CodeEpiphany.Settings.LeetCode",
      PluginBundle.message("leetcode.settings.displayName"),
      "CodeEpiphany.Settings.LeetCode.HelpTopic"
    ) {

  override def getSettings: BaseCodeDojoSettings.CodeDojoSettingsState =
    LeetCodeSettings.getInstance(myProject).getState

  override def supportedLanguages: List[(Language, LanguageVersion)] = LANGUAGES

  override def createDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[Any] =
    getDemoTemplate(language, languageVersion)
}

object LeetCodeSettingsConfigurable {
  private val codeTemplateJson =
    parse(
      StringUtil.join(
        IOUtils.readLines(
          Objects.requireNonNull(getClass.getResourceAsStream("/settings/leetcode.json")),
          StandardCharsets.UTF_8
        ),
        ""
      )
    ).toOption.get

  private val LANGUAGES: List[(Language, LanguageVersion)] =
    List(
      (Java, AnyVersion),
      (Javascript, AnyVersion),
      (Kotlin, AnyVersion),
      (Dart, AnyVersion),
      (Typescript, AnyVersion),
      (Erlang, AnyVersion),
      (Elixir, AnyVersion),
      (Cpp, AnyVersion),
      (PHP, AnyVersion),
      (Racket, AnyVersion),
      (Swift, AnyVersion),
      (Rust, AnyVersion),
      (Scala, AnyVersion),
      (CSharp, AnyVersion),
      (Cangjie, AnyVersion),
      (GO, AnyVersion),
      (Ruby, AnyVersion),
      (C, AnyVersion),
      (Python, AnyVersion),
      (Python, SpecificVersion("3"))
    ).sorted

  private def createCodeTemplate(): Map[(Language, LanguageVersion), LeetCodeChallengeCodeTemplate] = {
    LANGUAGES
      .map((language, languageVersion) => {
        val t = JsonPath.root
          .selectDynamic(s"${language.value}${languageVersion.version}")
          .string
          .getOption(codeTemplateJson)
        t.map { code =>
          (language, languageVersion) ->
            LeetCodeChallengeCodeTemplate(
              "4",
              "Interview 4",
              LeetCode,
              "Median of two sorted arrays",
              StringUtil.replace(code, "\\n", "\n"),
              "median-of-two-sorted-arrays",
              "Median of two sorted arrays",
              ChallengeDifficulty.Hard.value,
              language,
              languageVersion,
              LeetCodeChallengeData(
                questionId = "4",
                frontendQuestionId = "Interview 4",
                title = "Median of two sorted arrays",
                titleSlug = "median-of-two-sorted-arrays",
                content = "",
                translatedTitle = None,
                translatedContent = None,
                isPaidOnly = false,
                difficulty = "",
                likes = 0,
                dislikes = 0,
                isLiked = None,
                similarQuestions = "",
                exampleTestcases = "",
                topicTags = Nil,
                codeSnippets = Nil,
                hints = Nil,
                status = None,
                testCase = "",
                metaData = ""
              )
            )
        }
      })
      .collect { case Some(value) => value }
      .toMap
  }

  private val DEMOS_CODE_TEMPLATES = createCodeTemplate()

  def getDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[LeetCodeChallengeCodeTemplate] =
    DEMOS_CODE_TEMPLATES.get((language, languageVersion))

}
