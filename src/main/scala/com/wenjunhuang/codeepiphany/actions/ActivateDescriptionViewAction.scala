package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}

import com.wenjunhuang.codeepiphany.toolwindows.sidebar.SidebarWindowFactory
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, ProjectNonNull}

class ActivateDescriptionViewAction extends AnAction with ProjectNonNull with ActionCompatible{
  override def actionPerformed(e: AnActionEvent): Unit = {
    SidebarWindowFactory.activate(e.getProject, SidebarWindowFactory.DESCRIPTION)
  }

  override def update(e: AnActionEvent): Unit = {
    if !isSatisfied(e) then e.getPresentation.setEnabled(false)
    else e.getPresentation.setEnabled(true)
  }
}
