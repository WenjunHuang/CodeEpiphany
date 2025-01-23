package com.wenjunhuang.codeepiphany.editor.actions

import com.intellij.openapi.actionSystem.*

import com.wenjunhuang.codeepiphany.editor.actions.SubmitCodeAction.*

class RunCodeAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    getProvider(e) match
      case Some(provider) =>
        if provider.canRun then
          provider.runCurrent()
      case None =>
  }

  override def update(e: AnActionEvent): Unit = {
    val enabled = getProvider(e).exists(_.canRun)
    e.getPresentation.setEnabled(enabled)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  private def getProvider(e: AnActionEvent): Option[SubmitCodeProvider] = {
    Option(e.getData(PlatformCoreDataKeys.FILE_EDITOR)).flatMap { editor =>
      Option(SUBMITCODE_PROVIDER_KEY.get(editor))
    }
  }
}
