package com.wenjunhuang.codeepiphany.model

import icons.CodeEpiphanyIcons
import org.typelevel.ci.CIString

import javax.swing.Icon

enum LanguageVersion {
  case AnyVersion
  case SpecificVersion(value: String)

  def version: String = this match {
    case AnyVersion           => ""
    case SpecificVersion(ver) => ver
  }
}

enum Language(val value: String, val fileExt: String, val show: String, val icon: Icon) {
  case C          extends Language("c", "c","C", CodeEpiphanyIcons.Languages.C)
  case Clojure    extends Language("clojure", "clj","Clojure", CodeEpiphanyIcons.Languages.CLOJURE)
  case Cpp        extends Language("cpp", "cc","C++", CodeEpiphanyIcons.Languages.CPP)
  case CSharp     extends Language("csharp", "cs","C#", CodeEpiphanyIcons.Languages.CSHARP)
  case Dart       extends Language("dart", "dart","Dart", CodeEpiphanyIcons.Languages.DART)
  case ERLANG     extends Language("erlang", "erl","Erlang", CodeEpiphanyIcons.Languages.ERLANG)
  case GO         extends Language("go", "go","Go", CodeEpiphanyIcons.Languages.GO)
  case Haskell    extends Language("haskell", "hs","Haskell", CodeEpiphanyIcons.Languages.HASKELL)
  case Java       extends Language("java", "java","Java", CodeEpiphanyIcons.Languages.JAVA)
  case Javascript extends Language("javascript", "js","Javascript", CodeEpiphanyIcons.Languages.JAVASCRIPT)
  case Julia      extends Language("julia", "jl","Julia", CodeEpiphanyIcons.Languages.JULIA)
  case Kotlin     extends Language("kotlin", "kt", "Kotlin",CodeEpiphanyIcons.Languages.KOTLIN)
  case ObjectiveC extends Language("objectivec", "objc","ObjectiveC", CodeEpiphanyIcons.Languages.OBJECTIVEC)
  case Perl       extends Language("perl", "pl", "Perl",CodeEpiphanyIcons.Languages.PERL)
  case PHP        extends Language("php", "php","PHP", CodeEpiphanyIcons.Languages.PHP)
  case Pypy3      extends Language("pypy3", "py","Pypy3", CodeEpiphanyIcons.Languages.PYTHON)
  case Python3    extends Language("python3", "py", "Python3",CodeEpiphanyIcons.Languages.PYTHON)
  case R          extends Language("r", "r","R", CodeEpiphanyIcons.Languages.R)
  case Ruby       extends Language("ruby", "rb","Ruby", CodeEpiphanyIcons.Languages.RUBY)
  case Rust       extends Language("rust", "rs","Rust", CodeEpiphanyIcons.Languages.RUST)
  case Scala      extends Language("scala", "scala","Scala", CodeEpiphanyIcons.Languages.SCALA)
  case Swift      extends Language("swift", "swift","Swift", CodeEpiphanyIcons.Languages.SWIFT)
  case Typescript extends Language("typescript", "ts","Typescript", CodeEpiphanyIcons.Languages.TYPESCRIPT)
}

object Language {
  val ALL_LANGUAGES: Map[CIString, Language] =
    Language.values.map(v => CIString(v.value) -> v).toMap
  def fromCIString(str: CIString): Option[Language] = ALL_LANGUAGES.get(str)
}
