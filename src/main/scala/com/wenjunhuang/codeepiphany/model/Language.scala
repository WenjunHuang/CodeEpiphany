package com.wenjunhuang.codeepiphany.model

import icons.CodeEpiphanyIcons
import javax.swing.Icon
import org.typelevel.ci.CIString
import scala.collection.mutable

import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.model.LanguageVersion.{ AnyVersion, SpecificVersion }

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
  case Octave    extends Language("octave", "m", "Octave", CodeEpiphanyIcons.Languages.OCTAVE)

  def createComment(comment: String): String = this match {
    case C | Cpp | CSharp | D | GO | Java | Kotlin | ObjectiveC | PHP | Rust | Scala | Swift | Dart | Javascript |
        Typescript | Cangjie | Delphi | Pascal | FSharp | ReasonML =>
      s"//$comment"
    case Clojure | Racket | Asm | LLVM | Lisp =>
      s";$comment"
    case Pypy | Python | Ruby | Julia | Perl | R | Elixir =>
      s"#$comment"
    case Haskell | Lua =>
      s"--$comment"
    case Erlang | Octave =>
      s"%$comment"
    case OCaml =>
      s"(*$comment*)"
  }

  def encloseCodeInRegion(code: String): String =
    s"\n${createComment(Constants.SUBMIT_CODE_REGION_BEGIN)}\n$code\n${createComment(Constants.SUBMIT_CODE_REGION_END)}"

  def extractCodeFromRegion(code: String): String = {
    val begin = createComment(Constants.SUBMIT_CODE_REGION_BEGIN)
    val end   = createComment(Constants.SUBMIT_CODE_REGION_END)

    val result   = mutable.ListBuffer.empty[String]
    val accum    = mutable.ListBuffer.empty[String]
    var inRegion = false
    StringUtil.splitByLinesDontTrim(code).foreach { line =>
      val trimmed = line.trim
      if StringUtil.equals(trimmed, begin) then inRegion = true
      else if StringUtil.equals(trimmed, end) then
        if inRegion then
          inRegion = false
          result.addAll(accum)
          accum.clear()
      else if inRegion then accum += line
    }
    result.mkString("\n")
  }
}

object Language {

  implicit val ordering: Ordering[Language] = (x: Language, y: Language) => x.value.compareTo(y.value)

  private val ALL_LANGUAGES: Map[CIString, Language] =
    Language.values.map(v => CIString(v.value) -> v).toMap

  def fromCIString(str: CIString): Option[Language] = ALL_LANGUAGES.get(str)
}
