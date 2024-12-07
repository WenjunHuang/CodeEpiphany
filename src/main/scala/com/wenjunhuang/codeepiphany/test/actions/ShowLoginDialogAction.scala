package com.wenjunhuang.codeepiphany.test.actions

import cats.effect.SyncIO
import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent }
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.AskForLoginResult
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.ui.HackerRankLoginDialog
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.typelevel.log4cats.LoggerFactory

class ShowLoginDialogAction extends AnAction {
  private val myLogger = LoggerFactory[SyncIO].getLogger()
  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject
    val dialog = new HackerRankLoginDialog(
      project,
      {
        case Right(AskForLoginResult.Done)      => myLogger.info("Login success").unsafeRunSync()
        case Right(AskForLoginResult.Cancelled) => myLogger.info("Login cancel").unsafeRunSync()
        case Left(error)                        => println(s"Login failed: $error")
      }
    )
    dialog.show()
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}
