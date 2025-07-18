package com.wenjunhuang.codeepiphany.actions.webview

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull}

class ActualZoomAction extends AnAction with DataKeyNotNull(WebviewActionProvider.DATA_KEY) with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit = getValue(e).actualZoom()

  override def update(e: AnActionEvent): Unit = {
    if !isSatisfied(e) then e.getPresentation.setEnabled(false)
    else {
      getValue(e).zoom match
        case 100.0 => e.getPresentation.setEnabled(false)
        case _     => e.getPresentation.setEnabled(true)
    }
  }
}
