package com.wenjunhuang.codeepiphany.leetcode.settings

import io.circe.optics.JsonPath
import io.circe.parser.*
import java.nio.charset.StandardCharsets
import java.util.Objects
import org.apache.commons.io.IOUtils

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.leetcode.model.LeetCodeChallengeCodeTemplate
import com.wenjunhuang.codeepiphany.leetcode.settings.LeetCodeCNSettingsConfigurable.{getDemoTemplate, LANGUAGES}
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.LeetCodeCN
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.settings.dojo.{BaseCodeDojoSettings, BaseSettingsConfigurable}

class LeetCodeCNSettingsConfigurable(project: Project)
    extends BaseSettingsConfigurable(
      project,
      CodeDojo.LeetCodeCN,
      "CodeEpiphany.Settings.LeetCodeCN",
      PluginBundle.message("leetcodecn.settings.displayName"),
      "CodeEpiphany.Settings.LeetCodeCN.HelpTopic"
    ) {

  override def getSettings: BaseCodeDojoSettings.CodeDojoSettingsState =
    LeetCodeCNSettings.getInstance(myProject).getState

  override def supportedLanguages: List[(Language, LanguageVersion)] = LANGUAGES

  override def createDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[Any] =
    getDemoTemplate(language, languageVersion)
}

object LeetCodeCNSettingsConfigurable {
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
              LeetCodeCN,
              "寻找两个正序数组的中位数",
              StringUtil.replace(code, "\\n", "\n"),
              "median-of-two-sorted-arrays",
              "寻找两个正序数组的中位数",
              ChallengeDifficulty.Hard.value,
              language,
              languageVersion
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
