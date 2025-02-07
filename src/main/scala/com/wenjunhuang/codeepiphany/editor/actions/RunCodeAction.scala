package com.wenjunhuang.codeepiphany.editor.actions

import com.intellij.openapi.actionSystem.*

import com.wenjunhuang.codeepiphany.editor.actions.SubmitCodeAction.*

class RunCodeAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    getProvider(e) match
      case Some(provider) =>
        if provider.canRun then provider.runCurrent()
      case None =>
  }

  override def update(e: AnActionEvent): Unit = {
    getProvider(e) match
      case None => e.getPresentation.setEnabledAndVisible(false)
      case Some(provider) =>
        if provider.enabledRun then
          e.getPresentation.setVisible(true)
          e.getPresentation.setEnabled(provider.canRun)
        else e.getPresentation.setEnabledAndVisible(false)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  private def getProvider(e: AnActionEvent): Option[SubmitCodeProvider] = {
    Option(e.getData(PlatformCoreDataKeys.FILE_EDITOR)).flatMap { editor =>
      Option(SUBMITCODE_PROVIDER_KEY.get(editor))
    }
  }
}
