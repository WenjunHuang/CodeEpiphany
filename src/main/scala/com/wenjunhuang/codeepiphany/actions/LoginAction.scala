package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.*

import com.wenjunhuang.codeepiphany.actions.LoginAction.*
import com.wenjunhuang.codeepiphany.utils.actions.{AbstractLoadingAction, ActionCompatible, DataKeyNotNull}

class LoginAction extends AbstractLoadingAction with DataKeyNotNull(LOGIN_LOGOUT_KEY) with ActionCompatible {

  override def actionPerformed(e: AnActionEvent): Unit = 
    getValue(e).login()
  

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
