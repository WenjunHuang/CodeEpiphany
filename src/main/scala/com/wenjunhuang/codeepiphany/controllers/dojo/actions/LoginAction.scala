package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent }
import keys.LOGIN_LOGOUT_KEY

class LoginAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(LOGIN_LOGOUT_KEY.getData(e.getDataContext)).foreach(_.login())

  override def update(e: AnActionEvent): Unit =
    LOGIN_LOGOUT_KEY.getData(e.getDataContext) match {
      case null => e.getPresentation.setEnabledAndVisible(false)
      case alg =>
        alg.isLoggedIn().map {
          case true  => e.getPresentation.setEnabledAndVisible(false)
          case false => e.getPresentation.setEnabledAndVisible(true)
        }
    }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
