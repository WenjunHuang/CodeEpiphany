package com.wenjunhuang.codeepiphany.test.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.ui.HackerRankLoginDialog

class ShowLoginDialogAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val dialog = new HackerRankLoginDialog(project)
    dialog.show()
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}

