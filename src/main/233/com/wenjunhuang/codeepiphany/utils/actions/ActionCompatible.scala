package com.wenjunhuang.codeepiphany.utils.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction}

trait ActionCompatible { self: AnAction =>
  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.EDT
}
