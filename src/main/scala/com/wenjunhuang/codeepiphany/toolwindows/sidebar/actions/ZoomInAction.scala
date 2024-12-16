package com.wenjunhuang.codeepiphany.toolwindows.sidebar.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent }
import com.intellij.openapi.project.DumbAware
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.DescriptionView

class ZoomInAction extends AnAction with DumbAware {
  override def actionPerformed(e: AnActionEvent): Unit =
    e.getData(DescriptionView.DATA_KEY) match
      case null => ()
      case view => view.zoomIn()

  override def update(e: AnActionEvent): Unit =
    e.getData(DescriptionView.DATA_KEY) match
      case null => e.getPresentation.setEnabled(false)
      case view => e.getPresentation.setEnabled(view.canZoomIn)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}
