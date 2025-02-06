package com.wenjunhuang.codeepiphany.codeforces.settings

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.codeforces.models.{CodeForcesChallengeCodeTemplate, CodeForcesChallengeData}
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettingsConfigurable.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.settings.dojo.{BaseCodeDojoSettings, BaseSettingsConfigurable}

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

  override def supportedLanguages: List[(Language, LanguageVersion)] = CODEFORCES_LANGUAGES.keys.toList.sorted

  override def createDemoTemplate(
    language: Language,
    languageVersion: LanguageVersion
  ): Option[CodeForcesChallengeCodeTemplate] = Some(
    DEMO_CHALLENGE_TEMPLAGE.copy(language = language, languageVersion = languageVersion)
  )
}

object CodeForcesSettingsConfigurable {
  val CODEFORCES_LANGUAGES: Map[(Language, LanguageVersion), String] = Map(
    (Java, SpecificVersion("8"))        -> "36",
    (Java, SpecificVersion("21"))       -> "87",
    (C, SpecificVersion("11"))          -> "43",
    (Cpp, SpecificVersion("17"))        -> "54",
    (Cpp, SpecificVersion("20"))        -> "89",
    (Cpp, SpecificVersion("23"))        -> "91",
    (CSharp, SpecificVersion("8"))      -> "65",
    (CSharp, SpecificVersion("10"))     -> "79",
    (CSharp, SpecificVersion("Mono"))   -> "9",
    (Kotlin, SpecificVersion("1.7.20")) -> "83",
    (Kotlin, SpecificVersion("1.9.21")) -> "88",
    (OCaml, AnyVersion)                 -> "19",
    (Delphi, AnyVersion)                -> "3",
    (Pascal, AnyVersion)                -> "4",
    (D, AnyVersion)                     -> "28",
    (GO, AnyVersion)                    -> "32",
    (Haskell, AnyVersion)               -> "12",
    (Perl, AnyVersion)                  -> "13",
    (PHP, AnyVersion)                   -> "6",
    (Python, SpecificVersion("2"))      -> "7",
    (Python, SpecificVersion("3"))      -> "31",
    (Pypy, SpecificVersion("2"))        -> "40",
    (Pypy, SpecificVersion("3"))        -> "70",
    (Ruby, AnyVersion)                  -> "67",
    (Rust, AnyVersion)                  -> "75",
    (Scala, AnyVersion)                 -> "20",
    (Javascript, AnyVersion)            -> "55"
  )
  val CODEFORCES_LANGUAGES_REVERSE: Map[String, (Language, LanguageVersion)] = CODEFORCES_LANGUAGES.map(_.swap)

  val DEMO_CHALLENGE_TEMPLAGE: CodeForcesChallengeCodeTemplate = CodeForcesChallengeCodeTemplate(
    "2061I",
    "Kevin and Nivek",
    Java,
    SpecificVersion("8"),
    Some(3500),
    None,
    List("divide and conquer", "dp"),
    content = CodeForcesChallengeData(2061, "I", "Kevin and Nivek", Set((Java, SpecificVersion("21"))))
  )
}
