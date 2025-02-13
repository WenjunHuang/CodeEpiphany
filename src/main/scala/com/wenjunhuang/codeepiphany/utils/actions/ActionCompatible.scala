package com.wenjunhuang.codeepiphany.utils.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction }

import com.wenjunhuang.codeepiphany.utils.IdeUtils

trait ActionCompatible { self: AnAction =>
  override def getActionUpdateThread: ActionUpdateThread = {
    if IdeUtils.majorVersion >= 2024 then ActionUpdateThread.BGT
    else ActionUpdateThread.EDT
  }
}
