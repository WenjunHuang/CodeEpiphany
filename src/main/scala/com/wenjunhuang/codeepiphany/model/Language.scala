package com.wenjunhuang.codeepiphany.model

import org.typelevel.ci.CIString

enum LanguageVersion {
  case All
  case Specific(version: String)
}

enum Language(val value: String, val fileExt: String) {
  case C          extends Language("c", "c")
  case Clojure    extends Language("clojure", "clj")
  case Cpp        extends Language("cpp", "cc")
  case CSharp     extends Language("csharp", "cs")
  case Dart       extends Language("dart", "dart")
  case ERLANG     extends Language("erlang", "erl")
  case GO         extends Language("go", "go")
  case Haskell    extends Language("haskell", "hs")
  case Java       extends Language("java", "java")
  case Javascript extends Language("javascript", "js")
  case Julia      extends Language("julia", "jl")
  case Kotlin     extends Language("kotlin", "kt")
  case ObjectiveC extends Language("objectivec", "objc")
  case Perl       extends Language("perl", "pl")
  case PHP        extends Language("php", "php")
  case Pypy3      extends Language("pypy3", "py")
  case Python3    extends Language("python3", "py")
  case R          extends Language("r", "r")
  case Ruby       extends Language("ruby", "rb")
  case Rust       extends Language("rust", "rs")
  case Scala      extends Language("scala", "scala")
  case Swift      extends Language("swift", "swift")
  case Typescript extends Language("typescript", "ts")
}

object Language {
  val ALL_LANGUAGES: Map[CIString, Language] = Language.values.map(v => CIString(v.value) -> v).toMap
  def fromCIString(str: CIString): Option[Language] = ALL_LANGUAGES.get(str)
}
