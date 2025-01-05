package com.wenjunhuang.codeepiphany.editor.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.wenjunhuang.codeepiphany.editor.actions.providers.SubmitCodeProvider.SUBMITCODE_PROVIDER_KEY

class SubmitCodeAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    Option(SUBMITCODE_PROVIDER_KEY.getData(e.getDataContext)) match {
      case Some(provider) =>
        provider.runCurrent()
      case None =>
    }
  }

  override def update(e: AnActionEvent): Unit = {
    Option(SUBMITCODE_PROVIDER_KEY.getData(e.getDataContext)) match {
      case Some(provider) =>
        e.getPresentation.setEnabledAndVisible(true)
      case None =>
        e.getPresentation.setEnabledAndVisible(false)
    }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
