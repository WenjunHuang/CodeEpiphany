package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnActionEvent, DataKey }
import com.intellij.openapi.project.DumbAwareAction

import OpenSubmissionCodeAction.*

class OpenSubmissionCodeAction extends DumbAwareAction {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(OPEN_SUBMISSION_PROVIDER_KEY.getData(e.getDataContext)).foreach(_.openSubmissionCode())

  override def update(e: AnActionEvent): Unit = {
    Option(OPEN_SUBMISSION_PROVIDER_KEY.getData(e.getDataContext)) match
      case Some(provider) => e.getPresentation.setEnabledAndVisible(true)
      case None           => e.getPresentation.setEnabled(false)

  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

}

object OpenSubmissionCodeAction {
  final val OPEN_SUBMISSION_PROVIDER_KEY = DataKey.create[OpenSubmissionCodeProvider]("OPEN_SUBMISSION_PROVIDER_KEY")
  trait OpenSubmissionCodeProvider {
    def openSubmissionCode(): Unit
  }
}
