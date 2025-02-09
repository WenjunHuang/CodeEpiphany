package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.*

import com.wenjunhuang.codeepiphany.actions.LoginAction.*
import com.wenjunhuang.codeepiphany.utils.actions.{ AbstractLoadingAction, DataKeyNotNull }

class LoginAction extends AbstractLoadingAction with DataKeyNotNull(LOGIN_LOGOUT_KEY) {

  override def actionPerformed(e: AnActionEvent): Unit =
    Option(LOGIN_LOGOUT_KEY.getData(e.getDataContext)).foreach(_.login())

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    if isSatisfied(e) then
      val provider = getValue(e)

      presentation.setVisible(true)
      if provider.hasLoggedIn then presentation.setEnabledAndVisible(false)
      else if provider.isLoggingIn then
        presentation.setEnabled(false)
        setLoading(presentation, true)
      else
        presentation.setEnabled(true)
        setLoading(presentation, false)
    else presentation.setEnabledAndVisible(false)
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
