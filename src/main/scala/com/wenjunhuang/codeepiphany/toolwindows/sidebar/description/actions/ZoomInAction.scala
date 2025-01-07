package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.intellij.openapi.project.DumbAware
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.description.ChallengeDescriptionView

class ZoomInAction extends AnAction with DumbAware {
  override def actionPerformed(e: AnActionEvent): Unit =
    e.getData(ChallengeDescriptionView.DATA_KEY) match
      case null => ()
      case view => view.zoomIn()

  override def update(e: AnActionEvent): Unit =
    e.getData(ChallengeDescriptionView.DATA_KEY) match
      case null => e.getPresentation.setEnabled(false)
      case view => e.getPresentation.setEnabled(view.canZoomIn)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}
