package com.wenjunhuang.codeepiphany.codeforces.settings

import cats.syntax.all.*
import io.circe.optics.JsonPath
import io.circe.parser.*
import java.nio.charset.StandardCharsets
import java.util.Objects
import org.apache.commons.io.IOUtils

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.settings.dojo.{ BaseCodeDojoSettings, BaseSettingsConfigurable }
import CodeForcesSettingsConfigurable.*
import com.wenjunhuang.codeepiphany.codeforces.models.CodeForcesChallengeCodeTemplate

class CodeForcesSettingsConfigurable(project: Project)
    extends BaseSettingsConfigurable(
      project,
      CodeDojo.CodeForces,
      "CodeEpiphany.Settings.CodeForces",
      PluginBundle.message("codeforces.settings.displayName"),
      "CodeEpiphany.Settings.CodeForces.HelpTopic"
    ) {

  override def getSettings: BaseCodeDojoSettings.CodeDojoSettingsState =
    CodeForcesSettings.getInstance(myProject).getState

  override def supportedLanguages: List[(Language, LanguageVersion)] = CODEFORCES_LANGUAGES.map { case (l, lv, _) =>
    (l, lv)
  }

  override def createDemoTemplate(
    language: Language,
    languageVersion: LanguageVersion
  ): Option[CodeForcesChallengeCodeTemplate] = None
}

object CodeForcesSettingsConfigurable {
  val CODEFORCES_LANGUAGES: List[(Language, LanguageVersion, String)] = List(
    (Java, SpecificVersion("8"), "36"),
    (Java, SpecificVersion("21"), "87"),
    (C, SpecificVersion("11"), "43"),
    (Cpp, SpecificVersion("17"), "54"),
    (Cpp, SpecificVersion("20"), "89"),
    (Cpp, SpecificVersion("23"), "91"),
    (CSharp, SpecificVersion("8"), "65"),
    (CSharp, SpecificVersion("10"), "79"),
    (CSharp, SpecificVersion("Mono"), "9"),
    (Kotlin, SpecificVersion("1.7.20"), "83"),
    (Kotlin, SpecificVersion("1.9.21"), "88"),
    (OCaml, AnyVersion, "19"),
    (Delphi, AnyVersion, "3"),
    (Pascal, AnyVersion, "4"),
    (D, AnyVersion, "28"),
    (GO, AnyVersion, "32"),
    (Haskell, AnyVersion, "12"),
    (Perl, AnyVersion, "13"),
    (PHP, AnyVersion, "6"),
    (Python, SpecificVersion("2"), "7"),
    (Python, SpecificVersion("3"), "31"),
    (Pypy, SpecificVersion("2"), "40"),
    (Pypy, SpecificVersion("3"), "70"),
    (Ruby, AnyVersion, "67"),
    (Rust, AnyVersion, "75"),
    (Scala, AnyVersion, "20"),
    (Javascript, AnyVersion, "55")
  ).sorted

//  private val codeTemplateJson =
//    parse(
//      StringUtil.join(
//        IOUtils.readLines(
//          Objects.requireNonNull(getClass.getResourceAsStream("/settings/hackerrank.json")),
//          StandardCharsets.UTF_8
//        ),
//        ""
//      )
//    ).toOption.get
//
//  private def createCodeTemplate(): Map[(Language, LanguageVersion), HackerRankChallengeCodeTemplate] = {
//    CODEFORCES_LANGUAGES
//      .map((language, languageVersion,_) => {
//        val t = JsonPath.root
//          .selectDynamic(s"${language.value}${languageVersion.version}_template")
//          .string
//          .getOption(codeTemplateJson)
//        val h = JsonPath.root
//          .selectDynamic(s"${language.value}${languageVersion.version}_template_head")
//          .string
//          .getOption(codeTemplateJson)
//        val ta = JsonPath.root
//          .selectDynamic(s"${language.value}${languageVersion.version}_template_tail")
//          .string
//          .getOption(codeTemplateJson)
//        (t, h, ta).mapN { (template, header, tail) =>
//          (language, languageVersion) ->
//            HackerRankChallengeCodeTemplate(
//              "23074",
//              HackerRank,
//              "Sherlock and Permutations",
//              "sherlock-and-permutations",
//              "Help Sherlock in counting permutations.",
//              StringUtil.replace(header, "\\n", "\n"),
//              StringUtil.replace(template, "\\n", "\n"),
//              StringUtil.replace(tail, "\\n", "\n"),
//              Master.slug,
//              ChallengeDifficulty.Hard.value,
//              language,
//              languageVersion
//            )
//        }
//      })
//      .collect { case Some(value) => value }
//      .toMap
//  }
//
//  private val DEMOS_CODE_TEMPLATES = createCodeTemplate()
//
//  def getDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[HackerRankChallengeCodeTemplate] =
//    DEMOS_CODE_TEMPLATES.get((language, languageVersion))
//
}
