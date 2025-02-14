package com.wenjunhuang.codeepiphany.utils

import com.intellij.openapi.actionSystem.ActionToolbar

object ActionToolbarCompatibleUtils {
  def setToolBarWrapLayout(toolBar: ActionToolbar): Unit = {
    toolBar.setLayoutPolicy(ActionToolbar.WRAP_LAYOUT_POLICY)
  }

  def updateActions(toolBar: ActionToolbar): Unit = {
    toolBar.updateActionsImmediately()
  }
}
