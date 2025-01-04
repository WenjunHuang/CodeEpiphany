package com.wenjunhuang.codeepiphany.test.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent }
import com.intellij.openapi.vfs.LocalFileSystem
import com.wenjunhuang.codeepiphany.model.Repository
import com.wenjunhuang.codeepiphany.model.CodeDojo.{ HackerRank, LeetCode }

import java.io.File

class ChallengeSettingsTestAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val settings = Repository.getInstance(e.getProject)

  }

  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabledAndVisible(true)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
