package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.intellij.openapi.project.DumbAware

import com.wenjunhuang.codeepiphany.actions.LoginAction.*

class LogoutAction extends AnAction with DumbAware {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(LOGIN_LOGOUT_KEY.getData(e.getDataContext)).foreach(_.logout())

  override def update(e: AnActionEvent): Unit =
    val presentation = e.getPresentation
    LOGIN_LOGOUT_KEY.getData(e.getDataContext) match {
      case null => presentation.setEnabledAndVisible(false)
      case alg =>
        presentation.setEnabledAndVisible(alg.hasLoggedIn)
    }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
