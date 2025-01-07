package com.wenjunhuang.codeepiphany.toolwindows.sidebar.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.intellij.openapi.project.DumbAware
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.ChallengeDescriptionView

class ActualZoomAction extends AnAction with DumbAware {
  override def actionPerformed(e: AnActionEvent): Unit =
    e.getData(ChallengeDescriptionView.DATA_KEY) match
      case null => ()
      case view => view.actualZoom()

  override def update(e: AnActionEvent): Unit =
    e.getData(ChallengeDescriptionView.DATA_KEY) match
      case null => e.getPresentation.setEnabled(false)
      case view =>
        view.zoom match
          case 100.0 => e.getPresentation.setEnabled(false)
          case _     => e.getPresentation.setEnabled(true)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}
