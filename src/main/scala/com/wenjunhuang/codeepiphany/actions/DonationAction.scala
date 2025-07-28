package com.wenjunhuang.codeepiphany.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.wenjunhuang.codeepiphany.notifications.CodeEpiphanyNotification
import com.wenjunhuang.codeepiphany.utils.actions.ActionCompatible

class DonationAction extends DumbAwareAction with ActionCompatible {

  override def actionPerformed(e: AnActionEvent): Unit = {
    BrowserUtil.browse(CodeEpiphanyNotification.DONATION_URL)
  }
}
