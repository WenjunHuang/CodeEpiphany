package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnActionEvent}
import com.intellij.openapi.project.DumbAwareAction

import com.wenjunhuang.codeepiphany.actions.LoginAction.*
import com.wenjunhuang.codeepiphany.utils.actions.DataKeyNotNull

class LogoutAction extends DumbAwareAction with DataKeyNotNull(LOGIN_LOGOUT_KEY) {
  override def actionPerformed(e: AnActionEvent): Unit =
    getValue(e).logout()

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    if isSatisfied(e) then presentation.setEnabledAndVisible(getValue(e).hasLoggedIn)
    else presentation.setEnabledAndVisible(false)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
