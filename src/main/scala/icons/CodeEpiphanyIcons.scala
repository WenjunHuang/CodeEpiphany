package icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon

import javax.swing.Icon
import scala.annotation.static

trait CodeEpiphanyIcons {}

object CodeEpiphanyIcons {
  @static
  val LOGIN_ICON: Icon = IconLoader.getIcon("/icons/login.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  @static
  val LOGOUT_ICON: Icon = IconLoader.getIcon("/icons/logout.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  @static
  val QuestionToolWindowIcon: Icon = IconLoader.getIcon("/icons/file-lines-regular.svg", CodeEpiphanyIcons.getClass.getClassLoader)

  @static
  val DojoKeywordUIIcon: Icon = AllIcons.Actions.Search
  @static
  val DojoQueryParamUIIcon: Icon = AllIcons.Actions.ShortcutFilter
  
  @static
  val Loading: AnimatedIcon = AnimatedIcon.Default.INSTANCE
}
