package com.wenjunhuang.codeepiphany.hackerrank.settings

import cats.syntax.all.*
import com.intellij.openapi.options.ConfigurableBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.hackerrank.model.Contest.Master
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.model.{ChallengeCodeTemplate, ChallengeDifficulty, Language, LanguageVersion}
import io.circe.optics.JsonPath
import io.circe.parser.*
import org.apache.commons.io.IOUtils

import java.nio.charset.StandardCharsets
import java.util.Objects

class HackerRankSettingsConfigurable(private val myProject: Project)
    extends ConfigurableBase[HackerRankSettingsForm, HackerRankSettings.HackerRankSettingsState](
      "CodeEpiphany.Settings.HackerRank",
      PluginBundle.message("hackerrank.settings.displayName"),
      "CodeEpiphany.Settings.HackerRank.HelpTopic"
    ) {
  private val myDisposable = Disposer.newDisposable("HackerRankSettingsConfigurable")
  override def getSettings: HackerRankSettings.HackerRankSettingsState = {
    val settings = HackerRankSettings.getInstance(myProject)
    settings.getState
  }

  override def disposeUIResources(): Unit = {
    Disposer.dispose(myDisposable)
  }

  override def createUi(): HackerRankSettingsForm = HackerRankSettingsForm(myProject, myDisposable)
}

object HackerRankSettingsConfigurable {
  private val codeTemplateJson =
    parse(
      StringUtil.join(
        IOUtils.readLines(
          Objects.requireNonNull(getClass.getResourceAsStream("/settings/hackerrank.json")),
          StandardCharsets.UTF_8
        ),
        ""
      )
    ).toOption.get

  private def createCodeTemplate(): Map[(Language, LanguageVersion), ChallengeCodeTemplate] = {
    val keys = List(
      (Julia, AnyVersion),
      (Java, AnyVersion),
      (Java, SpecificVersion("8")),
      (Java, SpecificVersion("15")),
      (Javascript, AnyVersion),
      (R, AnyVersion),
      (Kotlin, AnyVersion),
      (Typescript, AnyVersion),
      (ERLANG, AnyVersion),
      (Cpp, AnyVersion),
      (Cpp, SpecificVersion("14")),
      (Cpp, SpecificVersion("20")),
      (PHP, AnyVersion),
      (Swift, AnyVersion),
      (Rust, AnyVersion),
      (Scala, AnyVersion),
      (Perl, AnyVersion),
      (CSharp, AnyVersion),
      (Haskell, AnyVersion),
      (GO, AnyVersion),
      (Ruby, AnyVersion),
      (Clojure, AnyVersion),
      (C, AnyVersion),
      (ObjectiveC, AnyVersion),
      (Python, AnyVersion),
      (Python, SpecificVersion("3")),
      (Pypy, AnyVersion),
      (Pypy, SpecificVersion("3"))
    )

    keys
      .map((language, languageVersion) => {
        val t = JsonPath.root
          .selectDynamic(s"${language.value}${languageVersion.version}_template")
          .string
          .getOption(codeTemplateJson)
        val h = JsonPath.root
          .selectDynamic(s"${language.value}${languageVersion.version}_template_head")
          .string
          .getOption(codeTemplateJson)
        val ta = JsonPath.root
          .selectDynamic(s"${language.value}${languageVersion.version}_template_tail")
          .string
          .getOption(codeTemplateJson)
        (t, h, ta).mapN { (template, header, tail) =>
          (language, languageVersion) ->
            ChallengeCodeTemplate(
              "23074",
              HackerRank,
              "Sherlock and Permutations",
              "sherlock-and-permutations",
              "Help Sherlock in counting permutations.",
              StringUtil.replace(header, "\\n", "\n"),
              StringUtil.replace(template, "\\n", "\n"),
              StringUtil.replace(tail, "\\n", "\n"),
              Master.slug,
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

  val HACKERRANK_LANGUAGES: Array[(Language, LanguageVersion)] = DEMOS_CODE_TEMPLATES.keys.toArray.sorted

  def getDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[ChallengeCodeTemplate] =
    DEMOS_CODE_TEMPLATES.get((language, languageVersion))

}
