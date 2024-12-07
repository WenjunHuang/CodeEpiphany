package com.wenjunhuang.codeepiphany.test.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.AskForLoginResult
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.ui.HackerRankLoginDialog

class ShowLoginDialogAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val dialog = new HackerRankLoginDialog(project,{
      case Right(AskForLoginResult.Done) => println("Login success")
      case Right(AskForLoginResult.Cancelled) => println("Login cancel")
      case Left(error) => println(s"Login failed: $error")
    })
    dialog.show()
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}

