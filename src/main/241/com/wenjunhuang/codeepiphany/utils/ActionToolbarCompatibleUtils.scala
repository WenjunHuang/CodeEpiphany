package com.wenjunhuang.codeepiphany.utils

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy

object ActionToolbarCompatibleUtils {
  def setToolBarWrapLayout(toolBar: ActionToolbar): Unit = {
    toolBar.setLayoutStrategy(ToolbarLayoutStrategy.WRAP_STRATEGY)
  }

  def updateActions(toolBar:ActionToolbar): Unit = {
    toolBar.updateActionsAsync()
  }
}
