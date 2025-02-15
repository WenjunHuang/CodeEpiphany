package com.wenjunhuang.codeepiphany.luogu.settings

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettingsConfigurable.LUOGU_LANGUAGES
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.settings.dojo.{ BaseCodeDojoSettings, BaseSettingsConfigurable }

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

  override def createDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[Any] = None
}

object LuoGuSettingsConfigurable {
  private val LUOGU_LANGUAGES: Map[(Language, LanguageVersion), String] =
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
  private val LUOGU_LANGUAGES_REVERSE: Map[String, (Language, LanguageVersion)] =
    LUOGU_LANGUAGES.map { case ((lang, version), id) => (id, (lang, version)) }
}
