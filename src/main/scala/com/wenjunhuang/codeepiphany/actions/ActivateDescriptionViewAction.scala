package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}

import com.wenjunhuang.codeepiphany.toolwindows.sidebar.SidebarWindowFactory

class ActivateDescriptionViewAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    if e.getProject != null then
      SidebarWindowFactory.activate(e.getProject,SidebarWindowFactory.DESCRIPTION)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def update(e: AnActionEvent): Unit = {
    if e.getProject != null then
      e.getPresentation.setEnabled(true)
    else
      e.getPresentation.setEnabled(false)
  }
}
