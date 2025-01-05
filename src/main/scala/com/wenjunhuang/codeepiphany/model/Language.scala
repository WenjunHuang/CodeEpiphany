package com.wenjunhuang.codeepiphany.model

import icons.CodeEpiphanyIcons
import org.typelevel.ci.CIString

import javax.swing.Icon

enum LanguageVersion {
  case AnyVersion
  case SpecificVersion(value: Int)

  def version: Int = this match {
    case AnyVersion           => 0
    case SpecificVersion(ver) => ver
  }
}

object LanguageVersion {
  implicit val ordering: Ordering[LanguageVersion] = (x: LanguageVersion, y: LanguageVersion) =>
    (x, y) match {
      case (AnyVersion, AnyVersion)                       => 0
      case (AnyVersion, SpecificVersion(_))               => -1
      case (SpecificVersion(_), AnyVersion)               => 1
      case (SpecificVersion(ver1), SpecificVersion(ver2)) => ver1.compareTo(ver2)
    }
}

enum Language(val value: String, val fileExt: String, val show: String, val icon: Icon) {
  case C          extends Language("c", "c", "C", CodeEpiphanyIcons.Languages.C)
  case Clojure    extends Language("clojure", "clj", "Clojure", CodeEpiphanyIcons.Languages.CLOJURE)
  case Cpp        extends Language("cpp", "cc", "C++", CodeEpiphanyIcons.Languages.CPP)
  case CSharp     extends Language("csharp", "cs", "C#", CodeEpiphanyIcons.Languages.CSHARP)
  case Dart       extends Language("dart", "dart", "Dart", CodeEpiphanyIcons.Languages.DART)
  case ERLANG     extends Language("erlang", "erl", "Erlang", CodeEpiphanyIcons.Languages.ERLANG)
  case GO         extends Language("go", "go", "Go", CodeEpiphanyIcons.Languages.GO)
  case Haskell    extends Language("haskell", "hs", "Haskell", CodeEpiphanyIcons.Languages.HASKELL)
  case Java       extends Language("java", "java", "Java", CodeEpiphanyIcons.Languages.JAVA)
  case Javascript extends Language("javascript", "js", "Javascript", CodeEpiphanyIcons.Languages.JAVASCRIPT)
  case Julia      extends Language("julia", "jl", "Julia", CodeEpiphanyIcons.Languages.JULIA)
  case Kotlin     extends Language("kotlin", "kt", "Kotlin", CodeEpiphanyIcons.Languages.KOTLIN)
  case ObjectiveC extends Language("objectivec", "objc", "ObjectiveC", CodeEpiphanyIcons.Languages.OBJECTIVEC)
  case Perl       extends Language("perl", "pl", "Perl", CodeEpiphanyIcons.Languages.PERL)
  case PHP        extends Language("php", "php", "PHP", CodeEpiphanyIcons.Languages.PHP)
  case Pypy3      extends Language("pypy3", "py", "Pypy3", CodeEpiphanyIcons.Languages.PYTHON)
  case Python3    extends Language("python3", "py", "Python3", CodeEpiphanyIcons.Languages.PYTHON)
  case R          extends Language("r", "r", "R", CodeEpiphanyIcons.Languages.R)
  case Ruby       extends Language("ruby", "rb", "Ruby", CodeEpiphanyIcons.Languages.RUBY)
  case Rust       extends Language("rust", "rs", "Rust", CodeEpiphanyIcons.Languages.RUST)
  case Scala      extends Language("scala", "scala", "Scala", CodeEpiphanyIcons.Languages.SCALA)
  case Swift      extends Language("swift", "swift", "Swift", CodeEpiphanyIcons.Languages.SWIFT)
  case Typescript extends Language("typescript", "ts", "Typescript", CodeEpiphanyIcons.Languages.TYPESCRIPT)

  def createComment(comment: String): String = this match {
    case C | Cpp | CSharp | GO | Java | Kotlin | ObjectiveC | PHP | Rust | Scala | Swift | Dart | Javascript |
        Typescript =>
      s"//$comment"
    case Clojure =>
      s";$comment"
    case Pypy3 | Python3 | Ruby | Julia | Perl | R =>
      s"#$comment"
    case Haskell =>
      s"--$comment"
    case ERLANG =>
      s"%$comment"
  }

  def makeCodeRegion(code: String): String =
    s"${createComment(Constants.SUBMIT_CODE_REGION_BEGIN)}\n$code\n${createComment(Constants.SUBMIT_CODE_REGION_END)}"
}

object Language {
  private val ALL_LANGUAGES: Map[CIString, Language] =
    Language.values.map(v => CIString(v.value) -> v).toMap
  def fromCIString(str: CIString): Option[Language] = ALL_LANGUAGES.get(str)
}
