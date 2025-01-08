package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.wenjunhuang.codeepiphany.model.Actions

class SubmissionLogView extends SimpleToolWindowPanel(true, true) {
  init()
  private def init():Unit = {
    val actionGroup = ActionManager.getInstance().getAction(Actions.SUBMISSIONS_TABLE_POPUP_GROUP)
  }
}
