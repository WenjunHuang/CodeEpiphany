package com.wenjunhuang.codeepiphany.actions

import icons.CodeEpiphanyIcons

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent, DataKey}

import com.wenjunhuang.codeepiphany.actions.LoginAction.*

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

object LoginAction {
  final val LOGIN_LOGOUT_KEY = DataKey.create[LoginLogoutProvider]("LOGIN_LOGOUT_KEY")
  trait LoginLogoutProvider {
    def login(): Unit
    def logout(): Unit
    def isLoggingIn: Boolean
    def hasLoggedIn: Boolean
  }

}
