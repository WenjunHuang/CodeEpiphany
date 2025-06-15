package com.wenjunhuang.codeepiphany.atcoder.settings

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderChallengeCodeTemplate
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettingsConfigurable.{ ATCODER_LANGUAGES, DEMO_TEMPLATE }
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.model.Language.*
import com.wenjunhuang.codeepiphany.model.LanguageVersion.*
import com.wenjunhuang.codeepiphany.settings.dojo.{ BaseCodeDojoSettings, BaseSettingsConfigurable }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

class AtCoderSettingsConfigurable(project: Project)
    extends BaseSettingsConfigurable(
      project,
      CodeDojo.AtCoder,
      "CodeEpiphany.Settings.AtCoder",
      PluginBundle.message("atcoder.settings.displayName"),
      "CodeEpiphany.Settings.AtCoder.HelpTopic"
    ) {
  override def supportedLanguages: List[(Language, LanguageVersion)] = ATCODER_LANGUAGES.keys.toList.sorted

  override def createDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[Any] =
    ATCODER_LANGUAGES.get((language, languageVersion)).map { _ =>
      DEMO_TEMPLATE.copy(language = language, languageVersion = languageVersion)
    }

  override def getSettings: BaseCodeDojoSettings.CodeDojoSettingsState = AtCoderSettings.getInstance(myProject).getState
}

object AtCoderSettingsConfigurable {
  val ATCODER_LANGUAGES: Map[(Language, LanguageVersion), String] = Map(
    (Cpp, SpecificVersion("20(GCC 12.2)"))                     -> "5001",
    (Cpp, SpecificVersion("23(GCC 12.2)"))                     -> "5028",
    (Cpp, SpecificVersion("23(CLANG 16.0.6)"))                 -> "5031",
    (GO, SpecificVersion("1.20.6"))                            -> "5002",
    (CSharp, SpecificVersion("11(.NET 7.0.7)"))                -> "5003",
    (Kotlin, SpecificVersion("1.8.20"))                        -> "5004",
    (Java, SpecificVersion("17"))                              -> "5005",
    (Javascript, SpecificVersion("(Node.js 18.6.1)"))          -> "5009",
    (Javascript, SpecificVersion("(Deno 1.35.1)"))             -> "5010",
    (R, SpecificVersion("4.2.1(GNU)"))                         -> "5011",
    (Swift, SpecificVersion("5.8.1"))                          -> "5014",
    (Dart, SpecificVersion("3.0.5"))                           -> "5015",
    (PHP, SpecificVersion("8.2.8"))                            -> "5016",
    (C, SpecificVersion("(GCC 12.2.0)"))                       -> "5017",
    (Ruby, SpecificVersion("3.2.2"))                           -> "5018",
    (FSharp, SpecificVersion("7.0(.NET 7.0.7)"))               -> "5021",
    (Haskell, SpecificVersion("(GHC 9.4.5)"))                  -> "5025",
    (Python, SpecificVersion("(SageMath 9.5)"))                -> "5033",
    (Perl, SpecificVersion("5.34"))                            -> "5037",
    (Asm, SpecificVersion("(NASM 2.15.05 x64)"))               -> "5040",
    (Pascal, SpecificVersion("3.2.2(fpc)"))                    -> "5041",
    (CSharp, SpecificVersion("11(.NET 7.0.7 AOT)"))            -> "5042",
    (Lua, SpecificVersion("5.4.6"))                            -> "5043",
    (Lua, SpecificVersion("(LuaJIT 2.1.0-beta3)"))             -> "5027",
    (Scala, SpecificVersion("3.3.0(Scala Native 0.4.14)"))     -> "5047",
    (Clojure, SpecificVersion("(babashka 1.3.181)"))           -> "5050",
    (Erlang, SpecificVersion("26.0.2"))                        -> "5051",
    (Typescript, SpecificVersion("5.1(Deno 1.35.1)"))          -> "5052",
    (Cpp, SpecificVersion("17(GCC 12.2)"))                     -> "5053",
    (Rust, SpecificVersion("1.70.0"))                          -> "5054",
    (Python, SpecificVersion("3.11.4(CPython)"))               -> "5055",
    (Scala, SpecificVersion("3.3.0(Dotty)"))                   -> "5056",
    (Typescript, SpecificVersion("5.1(Node.js 18.16.1)"))      -> "5058",
    (OCaml, SpecificVersion("5.0.0(ocamlopt)"))                -> "5059",
    (Python, SpecificVersion("3.10.10(Mambaforge / CPython)")) -> "5063",
    (Clojure, SpecificVersion("1.11.1"))                       -> "5064",
    (Clojure, SpecificVersion("(Carp 0.5.5)"))                 -> "5071",
    (Cpp, SpecificVersion("17(Clang 16.0.6)"))                 -> "5072",
    (Cpp, SpecificVersion("20(Clang 16.0.6)"))                 -> "5073",
    (LLVM, SpecificVersion("Clang 16.0.6"))                    -> "5074",
    (Lisp, SpecificVersion("(Common Lisp SBCL 2.3.6)"))        -> "5029",
    (Lisp, SpecificVersion("(Emacs Byte Compile 28.2)"))       -> "5075",
    (Lisp, SpecificVersion("(Emacs Native Compile 28.2)"))     -> "5062",
    (D, SpecificVersion("(GDC 12.2)"))                         -> "5077",
    (D, SpecificVersion("(DMD 2.104.0)"))                      -> "5012",
    (D, SpecificVersion("(LDC 1.32.2)"))                       -> "5013",
    (ReasonML, SpecificVersion("3.9.0"))                       -> "5081",
    (Python, SpecificVersion("Cython 0.29.34"))                -> "5082",
    (Python, SpecificVersion("PyPy 3.10-v7.3.12"))             -> "5078",
    (Octave, SpecificVersion("8.2.0"))                         -> "5083",
    (Elixir, SpecificVersion("1.15.2"))                        -> "5085"
  )

  val ATCODER_LANGUAGES_REVERSE: Map[String, (Language, LanguageVersion)] = ATCODER_LANGUAGES.map(_.swap)
  val DEMO_TEMPLATE = AtCoderChallengeCodeTemplate(
    contestId = "abc390",
    contestTitle = "AtCoder Beginner Contest 390",
    id = "abc390_g",
    problemIndex = "G",
    name = "Permutation Concatenation",
    title = "G. Permutation Concatenation",
    codeDojo = CodeDojo.AtCoder,
    language = Cpp,
    languageVersion = AnyVersion,
    description = "",
    record = null,
    content = null,
    testCases = List(
      ChallengeSettings.TestCase(input = "3\n1 2 3\n", expectedOutput = "1 2 3\n"),
      ChallengeSettings.TestCase(input = "4\n1 2 3 4\n", expectedOutput = "1 2 3 4\n")
    )
  )

}
