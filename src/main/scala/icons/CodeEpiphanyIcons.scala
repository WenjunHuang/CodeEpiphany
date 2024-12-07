package icons

import com.intellij.openapi.util.IconLoader

import javax.swing.Icon
import scala.annotation.static

trait CodeEpiphanyIcons {}

object CodeEpiphanyIcons {
  @static
  val LOGIN_ICON: Icon = IconLoader.getIcon("/icons/login.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  @static
  val LOGOUT_ICON: Icon = IconLoader.getIcon("/icons/logout.svg", CodeEpiphanyIcons.getClass.getClassLoader)
  @static
  val QuestionToolWindowIcon = IconLoader.getIcon("/icons/file-lines-regular.svg", CodeEpiphanyIcons.getClass.getClassLoader)
}
