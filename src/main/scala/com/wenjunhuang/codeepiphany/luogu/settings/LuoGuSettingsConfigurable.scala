package com.wenjunhuang.codeepiphany.luogu.settings

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.luogu.models.LuoGuChallengeCodeTemplate
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettingsConfigurable.{ DEMO_TEMPLATE, LUOGU_LANGUAGES }
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.settings.dojo.{ BaseCodeDojoSettings, BaseSettingsConfigurable }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

class LuoGuSettingsConfigurable(project: Project)
    extends BaseSettingsConfigurable(
      project,
      CodeDojo.LuoGu,
      "CodeEpiphany.Settings.LuoGu",
      PluginBundle.message("luogu.settings.displayName"),
      "CodeEpiphany.Settings.LuoGu.HelpTopic"
    ) {

  override def getSettings: BaseCodeDojoSettings.CodeDojoSettingsState =
    LuoGuSettings.getInstance(myProject).getState

  override def supportedLanguages: List[(Language, LanguageVersion)] = LUOGU_LANGUAGES.keys.toList.sorted

  override def createDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[Any] = Some(
    DEMO_TEMPLATE.copy(language = language, languageVersion = languageVersion)
  )
}

object LuoGuSettingsConfigurable {
  val LUOGU_LANGUAGES: Map[(Language, LanguageVersion), String] =
    Map(
      (Pascal, AnyVersion)                       -> "1",
      (C, AnyVersion)                            -> "2",
      (Cpp, SpecificVersion("14(GCC 9)"))        -> "28",
      (Cpp, SpecificVersion("98"))               -> "3",
      (Cpp, SpecificVersion("11"))               -> "4",
      (Cpp, SpecificVersion("14"))               -> "11",
      (Cpp, SpecificVersion("17"))               -> "12",
      (Cpp, SpecificVersion("20"))               -> "27",
      (Python, SpecificVersion("3"))             -> "7",
      (Pypy, SpecificVersion("3"))               -> "25",
      (Java, SpecificVersion("8"))               -> "8",
      (Java, SpecificVersion("21"))              -> "33",
      (Rust, AnyVersion)                         -> "15",
      (GO, AnyVersion)                           -> "14",
      (Haskell, AnyVersion)                      -> "19",
      (OCaml, AnyVersion)                        -> "30",
      (Julia, AnyVersion)                        -> "31",
      (Lua, AnyVersion)                          -> "32",
      (Kotlin, AnyVersion)                       -> "21",
      (Scala, AnyVersion)                        -> "22",
      (CSharp, SpecificVersion("(Mono)"))        -> "17",
      (Javascript, SpecificVersion("(Node.js)")) -> "9",
      (PHP, AnyVersion)                          -> "16",
      (Ruby, AnyVersion)                         -> "13",
      (Perl, AnyVersion)                         -> "23"
    )
  val LUOGU_LANGUAGES_REVERSE: Map[String, (Language, LanguageVersion)] =
    LUOGU_LANGUAGES.map { case ((lang, version), id) => (id, (lang, version)) }

  val DEMO_TEMPLATE = LuoGuChallengeCodeTemplate(
    id = "P11531",
    title = "[THUPC 2025 初赛] 检查站",
    language = Language.Cpp,
    languageVersion = LanguageVersion.SpecificVersion("17"),
    description = "",
    testCases = List(
      ChallengeSettings.TestCase(input = "3 2\n1 2 3\n1 2\n", expectedOutput = "1 2\n"),
      ChallengeSettings.TestCase(input = "5 3\n1 2 3 4 5\n1 2 3\n", expectedOutput = "1 2 3\n")
    )
  )
}
