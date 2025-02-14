package com.wenjunhuang.codeepiphany.utils.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction}
import com.intellij.util.text.VersionComparatorUtil

import com.wenjunhuang.codeepiphany.utils.IdeUtils

trait ActionCompatible { self: AnAction =>
  override def getActionUpdateThread: ActionUpdateThread = {
    if VersionComparatorUtil.compare(IdeUtils.shortVersion,"2024.1") >= 0 then ActionUpdateThread.BGT
    else ActionUpdateThread.EDT
  }
}
