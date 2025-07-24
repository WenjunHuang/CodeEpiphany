package com.wenjunhuang.codeepiphany.actions.webview

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.intellij.openapi.project.DumbAware
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull}

class ZoomInAction
    extends AnAction
    with DumbAware
    with DataKeyNotNull(WebviewActionProvider.DATA_KEY)
    with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit = {
    getValue(e).zoomIn()
  }

  override def update(e: AnActionEvent): Unit =
    if !isSatisfied(e) then e.getPresentation.setEnabled(false)
    else e.getPresentation.setEnabled(getValue(e).canZoomIn)
}
