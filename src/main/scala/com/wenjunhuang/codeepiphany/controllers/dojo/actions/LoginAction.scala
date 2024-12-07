package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent }
import keys.LOGIN_KEY

class LoginAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {}

  override def update(e: AnActionEvent): Unit = {
    val dataContext = e.getDataContext
    dataContext.getData(LOGIN_KEY) match {
      case null => e.getPresentation.setEnabled(false)
      case _    => e.getPresentation.setEnabled(true)
    }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}
