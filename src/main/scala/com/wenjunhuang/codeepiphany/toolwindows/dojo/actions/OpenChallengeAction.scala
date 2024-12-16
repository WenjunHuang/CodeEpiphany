package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}

class OpenChallengeAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit =  {
    e.getProject
  }
  
}
