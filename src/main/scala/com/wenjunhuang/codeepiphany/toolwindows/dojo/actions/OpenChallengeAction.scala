package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.CHALLENGE_PROVIDER_KEY

class OpenChallengeAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit =  {
    Option(CHALLENGE_PROVIDER_KEY.getData(e.getDataContext)).foreach{provider=>
      provider.openCurrentSelectedChallenge()
    }
  }

  override def update(e: AnActionEvent): Unit = {
    Option(CHALLENGE_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => e.getPresentation.setEnabledAndVisible(false)
      case Some(provider) => e.getPresentation.setEnabledAndVisible(true)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
