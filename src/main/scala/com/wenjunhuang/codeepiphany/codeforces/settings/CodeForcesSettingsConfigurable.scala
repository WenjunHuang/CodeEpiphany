package com.wenjunhuang.codeepiphany.codeforces.settings

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.codeforces.models.{ CodeForcesChallengeCodeTemplate, CodeForcesChallengeData }
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettingsConfigurable.*
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.settings.dojo.{ BaseCodeDojoSettings, BaseSettingsConfigurable }

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
    (Java, SpecificVersion("8(32bit)"))                     -> "36",
    (Java, SpecificVersion("21(64bit)"))                    -> "87",
    (C, SpecificVersion("11(GCC 5.1.0)"))                   -> "43",
    (Cpp, SpecificVersion("17(GCC 7.3.0)"))                 -> "54",
    (Cpp, SpecificVersion("20(GCC 13.2)"))                  -> "89",
    (Cpp, SpecificVersion("23(GCC 14.2)"))                  -> "91",
    (CSharp, SpecificVersion("8(.NET Core 3.1)"))           -> "65",
    (CSharp, SpecificVersion("10(.NET SDK 6.0)"))           -> "79",
    (CSharp, SpecificVersion("(Mono 6.8)"))                 -> "9",
    (Kotlin, SpecificVersion("1.7.20"))                     -> "83",
    (Kotlin, SpecificVersion("1.9.21"))                     -> "88",
    (OCaml, SpecificVersion("4.02.1"))                      -> "19",
    (Delphi, SpecificVersion("7"))                          -> "3",
    (Pascal, SpecificVersion("3.2.2(Free)"))                -> "4",
    (D, SpecificVersion("(DMD32 v2.105.0)"))                -> "28",
    (GO, SpecificVersion("1.22.2"))                         -> "32",
    (Haskell, SpecificVersion("(GHC 8.10.1)"))              -> "12",
    (Perl, SpecificVersion("5.20.1"))                       -> "13",
    (PHP, SpecificVersion("8.1.7"))                         -> "6",
    (Python, SpecificVersion("2.7.18"))                     -> "7",
    (Python, SpecificVersion("3.8.10"))                     -> "31",
    (Pypy, SpecificVersion("2.7.13(7.3.0)"))                -> "40",
    (Pypy, SpecificVersion("3.6.9(7.3.0)"))                 -> "41",
    (Pypy, SpecificVersion("3.10(7.3.15 64bit)"))           -> "70",
    (Ruby, SpecificVersion("3.2.2"))                        -> "67",
    (Rust, SpecificVersion("1.75.0(2021)"))                 -> "75",
    (Scala, SpecificVersion("2.12.8"))                      -> "20",
    (Javascript, SpecificVersion("(V8 4.8.0)"))             -> "34",
    (Javascript, SpecificVersion("(Node.js 15.8.0 64bit)")) -> "55"
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
