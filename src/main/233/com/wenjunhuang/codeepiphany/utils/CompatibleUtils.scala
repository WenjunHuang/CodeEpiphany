package com.wenjunhuang.codeepiphany.utils

import com.intellij.openapi.actionSystem.ActionToolbar

object CompatibleUtils {
  inline def setToolBarWrapLayout(toolBar: ActionToolbar): Unit = {
    toolBar.setLayoutPolicy(ActionToolbar.WRAP_LAYOUT_POLICY)
  }

  inline def updateActions(toolBar: ActionToolbar): Unit = {
    toolBar.updateActionsImmediately()
  }
}
