package icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon

import javax.swing.Icon
import scala.annotation.static

trait CodeEpiphanyIcons {}

object CodeEpiphanyIcons {
  @static
  val LOGIN: Icon =
    IconLoader.getIcon("/icons/login.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  @static
  val LOGOUT: Icon =
    IconLoader.getIcon("/icons/logout.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val RUN: Icon = IconLoader.getIcon("/icons/run.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val SUBMIT: Icon = IconLoader.getIcon("/icons/submit.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val QuestionToolWindowIcon: Icon =
    IconLoader.getIcon("/icons/file-lines-regular.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val SEARCH: Icon = AllIcons.Actions.Search
  @static
  val QUERY_PARAM: Icon = AllIcons.General.Filter

  @static
  val LOADING: AnimatedIcon = AnimatedIcon.Default.INSTANCE

  object Dojos {
    val LEETCODE: Icon   = IconLoader.getIcon("/icons/dojos/leetcode.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    val HACKERRANK: Icon = IconLoader.getIcon("/icons/dojos/hackerrank.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  }

  object Languages {
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
    final val DART: Icon =
      IconLoader.getIcon("/icons/languages/dart.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val ERLANG: Icon =
      IconLoader.getIcon("/icons/languages/erlang.svg", CodeEpiphanyIcons.getClass.getClassLoader)
    final val ELIXIR: Icon =
      IconLoader.getIcon("/icons/languages/elixir.svg", CodeEpiphanyIcons.getClass.getClassLoader)
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
  }
}
