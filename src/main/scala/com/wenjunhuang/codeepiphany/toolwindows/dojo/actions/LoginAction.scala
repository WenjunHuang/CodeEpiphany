package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent }
import com.intellij.ui.AnimatedIcon
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.LOGIN_LOGOUT_KEY
import icons.CodeEpiphanyIcons

class LoginAction extends AnAction {

  override def actionPerformed(e: AnActionEvent): Unit =
    Option(LOGIN_LOGOUT_KEY.getData(e.getDataContext)).foreach(_.login())

  override def update(e: AnActionEvent): Unit =
    LOGIN_LOGOUT_KEY.getData(e.getDataContext) match {
      case null =>
        e.getPresentation.setEnabledAndVisible(false)
      case provider =>
        e.getPresentation.setVisible(true)
        if provider.hasLoggedIn then e.getPresentation.setEnabledAndVisible(false)
        else if provider.isLoggingIn then
          e.getPresentation.setEnabled(false)
          e.getPresentation.setIcon(CodeEpiphanyIcons.LOADING)
        else
          e.getPresentation.setEnabled(true)
          e.getPresentation.setIcon(CodeEpiphanyIcons.LOGIN)
    }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
