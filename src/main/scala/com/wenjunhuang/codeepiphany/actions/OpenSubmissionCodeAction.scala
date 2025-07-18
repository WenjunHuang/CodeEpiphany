package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, DataKey}
import com.wenjunhuang.codeepiphany.actions.OpenSubmissionCodeAction.*
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull}

class OpenSubmissionCodeAction
    extends AnAction
    with DataKeyNotNull(OPEN_SUBMISSION_PROVIDER_KEY)
    with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit =
    getValue(e).openSubmissionCode()

  override def update(e: AnActionEvent): Unit = {
    if isSatisfied(e) then e.getPresentation.setEnabledAndVisible(true)
    else e.getPresentation.setEnabled(false)
  }

}

object OpenSubmissionCodeAction {
  final val OPEN_SUBMISSION_PROVIDER_KEY = DataKey.create[OpenSubmissionCodeProvider]("OPEN_SUBMISSION_PROVIDER_KEY")
  trait OpenSubmissionCodeProvider {
    def openSubmissionCode(): Unit
  }
}
