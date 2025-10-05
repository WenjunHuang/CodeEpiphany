package com.wenjunhuang.codeepiphany.model

import com.intellij.openapi.util.text.StringUtil
import com.wenjunhuang.codeepiphany.model.Language.escapeRegex
import com.wenjunhuang.codeepiphany.model.LanguageVersion.SpecificVersion
import icons.CodeEpiphanyIcons
import org.typelevel.ci.CIString

import javax.swing.Icon
import scala.collection.mutable
import scala.util.matching.Regex

enum LanguageVersion {
  case AnyVersion
  case SpecificVersion(value: String)

  def version: String = this match {
    case AnyVersion           => ""
    case SpecificVersion(ver) => ver
  }
}

object LanguageVersion {
  def fromString(ver: String): LanguageVersion = ver match {
    case null | "" => AnyVersion
    case _         => SpecificVersion(ver)
  }
  implicit val ordering: Ordering[LanguageVersion] = (x: LanguageVersion, y: LanguageVersion) =>
    (x, y) match {
      case (AnyVersion, AnyVersion)         => 0
      case (AnyVersion, SpecificVersion(_)) => -1
      case (SpecificVersion(_), AnyVersion) => 1
      case (SpecificVersion(ver1), SpecificVersion(ver2)) =>
        ver1.toIntOption.flatMap(v1 => ver2.toIntOption.map(v2 => v1.compareTo(v2))).getOrElse(ver1.compareTo(ver2))
    }
}

enum Language(val value: String, val fileExt: String, val show: String, val icon: Icon) {
  case Asm        extends Language("asm", "asm", "Assembly", CodeEpiphanyIcons.Languages.ASM)
  case C          extends Language("c", "c", "C", CodeEpiphanyIcons.Languages.C)
  case Clojure    extends Language("clojure", "clj", "Clojure", CodeEpiphanyIcons.Languages.CLOJURE)
  case Cpp        extends Language("cpp", "cc", "C++", CodeEpiphanyIcons.Languages.CPP)
  case CSharp     extends Language("csharp", "cs", "C#", CodeEpiphanyIcons.Languages.CSHARP)
  case Cangjie    extends Language("cangjie", "cj", "Cangjie", CodeEpiphanyIcons.Languages.CANGJIE)
  case Dart       extends Language("dart", "dart", "Dart", CodeEpiphanyIcons.Languages.DART)
  case Delphi     extends Language("delphi", "pas", "Delphi", CodeEpiphanyIcons.Languages.DELPHI)
  case Pascal     extends Language("pascal", "pas", "Pascal", CodeEpiphanyIcons.Languages.PASCAL)
  case D          extends Language("d", "d", "D", CodeEpiphanyIcons.Languages.DLANG)
  case Erlang     extends Language("erlang", "erl", "Erlang", CodeEpiphanyIcons.Languages.ERLANG)
  case Elixir     extends Language("elixir", "ex", "Elixir", CodeEpiphanyIcons.Languages.ELIXIR)
  case FSharp     extends Language("fsharp", "fs", "F#", CodeEpiphanyIcons.Languages.FSHARP)
  case GO         extends Language("go", "go", "Go", CodeEpiphanyIcons.Languages.GO)
  case Haskell    extends Language("haskell", "hs", "Haskell", CodeEpiphanyIcons.Languages.HASKELL)
  case Java       extends Language("java", "java", "Java", CodeEpiphanyIcons.Languages.JAVA)
  case Javascript extends Language("javascript", "js", "Javascript", CodeEpiphanyIcons.Languages.JAVASCRIPT)
  case Julia      extends Language("julia", "jl", "Julia", CodeEpiphanyIcons.Languages.JULIA)
  case Kotlin     extends Language("kotlin", "kt", "Kotlin", CodeEpiphanyIcons.Languages.KOTLIN)
  case ObjectiveC extends Language("objectivec", "objc", "ObjectiveC", CodeEpiphanyIcons.Languages.OBJECTIVEC)
  case OCaml      extends Language("ocaml", "ml", "OCaml", CodeEpiphanyIcons.Languages.OCAML)
  case Perl       extends Language("perl", "pl", "Perl", CodeEpiphanyIcons.Languages.PERL)
  case PHP        extends Language("php", "php", "PHP", CodeEpiphanyIcons.Languages.PHP)
  case Pypy       extends Language("pypy", "py", "Pypy", CodeEpiphanyIcons.Languages.PYTHON)
  case Python     extends Language("python", "py", "Python", CodeEpiphanyIcons.Languages.PYTHON)
  case R          extends Language("r", "r", "R", CodeEpiphanyIcons.Languages.R)
  case Racket     extends Language("racket", "rkt", "Racket", CodeEpiphanyIcons.Languages.RACKET)
  case Ruby       extends Language("ruby", "rb", "Ruby", CodeEpiphanyIcons.Languages.RUBY)
  case Rust       extends Language("rust", "rs", "Rust", CodeEpiphanyIcons.Languages.RUST)
  case Scala      extends Language("scala", "scala", "Scala", CodeEpiphanyIcons.Languages.SCALA)
  case Swift      extends Language("swift", "swift", "Swift", CodeEpiphanyIcons.Languages.SWIFT)
  case Typescript extends Language("typescript", "ts", "Typescript", CodeEpiphanyIcons.Languages.TYPESCRIPT)
  case Lua        extends Language("lua", "lua", "Lua", CodeEpiphanyIcons.Languages.LUA)
  case LLVM       extends Language("llvm", "ll", "LLVM", CodeEpiphanyIcons.Languages.LLVM)
  case Lisp       extends Language("lisp", "lisp", "Lisp", CodeEpiphanyIcons.Languages.LISP)
  case ReasonML   extends Language("reasonml", "re", "ReasonML", CodeEpiphanyIcons.Languages.REASONML)
  case Octave     extends Language("octave", "m", "Octave", CodeEpiphanyIcons.Languages.OCTAVE)
  case MySQL      extends Language("mysql", "sql", "MySQL", CodeEpiphanyIcons.Languages.MYSQL)
  case PostgreSQL extends Language("postgresql", "sql", "PostgreSQL", CodeEpiphanyIcons.Languages.POSTGRESQL)
  case OracleSQL  extends Language("oraclesql", "sql", "OracleSQL", CodeEpiphanyIcons.Languages.ORACLESQL)
  case MSSQL      extends Language("mssql", "sql", "MSSQL", CodeEpiphanyIcons.Languages.MSSQL)
  case Pandas     extends Language("pandas", "py", "Pandas", CodeEpiphanyIcons.Languages.PANDAS)

  private def singleLineCommentPrefix(): String = this match
    case C | Cpp | CSharp | D | GO | Java | Kotlin | ObjectiveC | PHP | Rust | Scala | Swift | Dart | Javascript |
        Typescript | Cangjie | Delphi | Pascal | FSharp | ReasonML =>
      "//"
    case Clojure | Racket | Asm | LLVM | Lisp =>
      ";"
    case Pypy | Python | Ruby | Julia | Perl | R | Elixir | Pandas =>
      "#"
    case Haskell | Lua | MySQL | PostgreSQL | OracleSQL | MSSQL =>
      "--"
    case Erlang | Octave =>
      "%"
    case OCaml =>
      s"(*"

  private def singleLineCommentPostfix(): String = this match
    case OCaml => "*)"
    case _     => ""

  def createComment(comment: String): String =
    s"${singleLineCommentPrefix()}$comment${singleLineCommentPostfix()}"

  def encloseCodeInRegion(code: String): String =
    s"\n${createComment(Constants.SUBMIT_CODE_REGION_BEGIN)}\n$code\n${createComment(Constants.SUBMIT_CODE_REGION_END)}"

  def extractCodeFromRegion(code: String): String = {
    val result   = mutable.ListBuffer.empty[String]
    val accum    = mutable.ListBuffer.empty[String]
    var inRegion = false
    StringUtil.splitByLinesKeepSeparators(code).foreach { line =>
      val trimmed = line.trim
      if matchRegion(trimmed, Constants.SUBMIT_CODE_REGION_BEGIN) then inRegion = true
      else if matchRegion(trimmed, Constants.SUBMIT_CODE_REGION_END) then
        if inRegion then
          inRegion = false
          result.addAll(accum)
          accum.clear()
      else if inRegion then accum += line
    }
    result.mkString("")
  }

  def matchRegion(comment: String, region: String): Boolean = {
    val pattern = s"""^${escapeRegex(singleLineCommentPrefix())}\\s*${escapeRegex(region)}\\s*${escapeRegex(
        singleLineCommentPostfix()
      )}$$""".r
    pattern.matches(comment)
  }
}

object Language {

  implicit val ordering: Ordering[Language] = (x: Language, y: Language) => x.value.compareTo(y.value)

  private val ALL_LANGUAGES: Map[CIString, Language] =
    Language.values.map(v => CIString(v.value) -> v).toMap

  def fromCIString(str: CIString): Option[Language] = ALL_LANGUAGES.get(str)

  def prettyPrint(lang: Language, langVer: LanguageVersion): String = {
    langVer.version match
      case numVersionWithNote(version, note) => s"${lang.show}$version $note"
      case numVersion(version)               => s"${lang.show}$version"
      case onlyNote(note)                    => s"${lang.show} $note"
      case v                                 => s"${lang.show} $v"
  }

  def escapeRegex(input: String): String = {
    val regexSpecialChars = """([\[\]\\\^\$\.\|\?\*\+\(\)\{\}])"""
    input.replaceAll(regexSpecialChars, "\\\\$1")
  }

  private val numVersionWithNote: Regex = """^([\d\.]+)\((.*)\)$""".r
  private val numVersion: Regex         = """^([\d\.]+)$""".r
  private val onlyNote: Regex           = """^\((.+)\)$""".r
}
