package icons

import javax.swing.Icon
import scala.annotation.static

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon

trait CodeEpiphanyIcons {}

object CodeEpiphanyIcons {
  @static
  val PLUGIN: Icon = IconLoader.getIcon("/icons/pluginIcon.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val DOWNLOAD: Icon = IconLoader.getIcon("/icons/download.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val LOGIN: Icon =
    IconLoader.getIcon("/icons/login.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  @static
  val LOGOUT: Icon =
    IconLoader.getIcon("/icons/logout.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val BUILDING: Icon =
    IconLoader.getIcon("/icons/building.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val RUN: Icon = IconLoader.getIcon("/icons/run.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val SUBMIT: Icon = IconLoader.getIcon("/icons/submit.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val SOLUTION: Icon = IconLoader.getIcon("/icons/solution.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val SUBMISSIONS: Icon = IconLoader.getIcon("/icons/submissions.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val CONSOLE: Icon = IconLoader.getIcon("/icons/console.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val SEARCH: Icon = IconLoader.getIcon("/icons/search.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val QUERY_PARAM: Icon = IconLoader.getIcon("/icons/filter.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val DESCRIPTION: Icon = IconLoader.getIcon("/icons/book.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val APP_BRACKETS: Icon = IconLoader.getIcon("/icons/app-brackets.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val LOADING: AnimatedIcon = AnimatedIcon.Default.INSTANCE

  object Dojos {
    val LEETCODE: Icon   = IconLoader.getIcon("/icons/dojos/leetcode.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    val HACKERRANK: Icon = IconLoader.getIcon("/icons/dojos/hackerrank.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    val CODEFORCES: Icon = IconLoader.getIcon("/icons/dojos/codeforces.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    val ATCODER: Icon    = IconLoader.getIcon("/icons/dojos/atcoder.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    val LUOGU: Icon      = IconLoader.getIcon("/icons/dojos/luogu.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  }

  object Languages {
    final val ASM: Icon =
      IconLoader.getIcon("/icons/languages/asm.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val C: Icon =
      IconLoader.getIcon("/icons/languages/c.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val CLOJURE: Icon =
      IconLoader.getIcon("/icons/languages/clojure.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val CPP: Icon =
      IconLoader.getIcon("/icons/languages/cpp.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val CSHARP: Icon =
      IconLoader.getIcon("/icons/languages/csharp.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val CANGJIE: Icon =
      IconLoader.getIcon("/icons/languages/cangjie.svg", CodeEpiphanyIcons.getClass.getClassLoader)

    final val DLANG: Icon = IconLoader.getIcon("/icons/languages/dlang.svg", CodeEpiphanyIcons.getClass.getClassLoader)

    final val PASCAL: Icon =
      IconLoader.getIcon("/icons/languages/freepascal.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val DELPHI: Icon =
      IconLoader.getIcon("/icons/languages/delphi.svg", CodeEpiphanyIcons.getClass.getClassLoader)

    final val DART: Icon =
      IconLoader.getIcon("/icons/languages/dart.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val ERLANG: Icon =
      IconLoader.getIcon("/icons/languages/erlang.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val ELIXIR: Icon =
      IconLoader.getIcon("/icons/languages/elixir.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val FSHARP: Icon =
      IconLoader.getIcon("/icons/languages/fsharp.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val GO: Icon =
      IconLoader.getIcon("/icons/languages/go.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val HASKELL: Icon =
      IconLoader.getIcon("/icons/languages/haskell.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val JAVA: Icon =
      IconLoader.getIcon("/icons/languages/java.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val JAVASCRIPT: Icon =
      IconLoader.getIcon("/icons/languages/javascript.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val JULIA: Icon =
      IconLoader.getIcon("/icons/languages/julia.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val KOTLIN: Icon =
      IconLoader.getIcon("/icons/languages/kotlin.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val OBJECTIVEC: Icon =
      IconLoader.getIcon("/icons/languages/objectivec.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val OCAML: Icon =
      IconLoader.getIcon("/icons/languages/ocaml.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val PERL: Icon =
      IconLoader.getIcon("/icons/languages/perl.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val PHP: Icon =
      IconLoader.getIcon("/icons/languages/php.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val PYTHON: Icon =
      IconLoader.getIcon("/icons/languages/python.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val R: Icon =
      IconLoader.getIcon("/icons/languages/r.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val RUBY: Icon =
      IconLoader.getIcon("/icons/languages/ruby.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val RUST: Icon =
      IconLoader.getIcon("/icons/languages/rust.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val RACKET: Icon =
      IconLoader.getIcon("/icons/languages/racket.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val SCALA: Icon =
      IconLoader.getIcon("/icons/languages/scala.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val SWIFT: Icon =
      IconLoader.getIcon("/icons/languages/swift.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val TYPESCRIPT: Icon =
      IconLoader.getIcon("/icons/languages/typescript.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val LUA: Icon =
      IconLoader.getIcon("/icons/languages/lua.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val LLVM: Icon =
      IconLoader.getIcon("/icons/languages/llvm.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val LISP: Icon =
      IconLoader.getIcon("/icons/languages/lisp.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val REASONML: Icon =
      IconLoader.getIcon("/icons/languages/reasonml.svg", CodeEpiphanyIcons.getClass.getClassLoader)

    final val OCTAVE: Icon =
      IconLoader.getIcon("/icons/languages/octave.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  }
}
