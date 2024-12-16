package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.LOGIN_LOGOUT_KEY

class LoginAction extends AnAction {
  
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(LOGIN_LOGOUT_KEY.getData(e.getDataContext)).foreach(_.login())

  override def update(e: AnActionEvent): Unit =
    LOGIN_LOGOUT_KEY.getData(e.getDataContext) match {
      case null => 
        e.getPresentation.setEnabledAndVisible(false)
      case alg =>
        e.getPresentation.setEnabledAndVisible(!alg.isLoggedIn)
    }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
