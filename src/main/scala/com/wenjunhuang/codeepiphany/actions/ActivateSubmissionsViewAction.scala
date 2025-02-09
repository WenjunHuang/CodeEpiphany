package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent }

import com.wenjunhuang.codeepiphany.toolwindows.sidebar.SidebarWindowFactory
import com.wenjunhuang.codeepiphany.utils.actions.ProjectNonNull

class ActivateSubmissionsViewAction extends AnAction with ProjectNonNull {
  override def actionPerformed(e: AnActionEvent): Unit = {
    SidebarWindowFactory.activate(e.getProject, SidebarWindowFactory.SUBMISSIONS)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(e: AnActionEvent): Unit = {
    if isSatisfied(e) then e.getPresentation.setEnabled(true)
    else e.getPresentation.setEnabled(false)
  }
}
