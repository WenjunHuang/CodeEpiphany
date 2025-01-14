package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent, DataKey}

class RefreshAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    Option(RefreshAction.REFRESH_PROVIDER_KEY.getData(e.getDataContext)).foreach(_.refresh())
  }

  override def update(e: AnActionEvent): Unit = {
    Option(RefreshAction.REFRESH_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case _    => e.getPresentation.setEnabled(true)
    }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object RefreshAction {
  val REFRESH_PROVIDER_KEY = DataKey.create[RefreshProvider]("REFRESH_PROVIDER_KEY")
  trait RefreshProvider {
    def refresh(): Unit
  }
}
