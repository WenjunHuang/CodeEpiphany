package com.wenjunhuang.codeepiphany.controllers.dojo

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, DataSink, DefaultActionGroup}
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.groups.*
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.{LISTS_PROVIDER_KEY, LOGIN_LOGOUT_KEY}

class HackerRankView(private val myProject: Project, private val myPresenter: HackerRankPresenter) extends SimpleToolWindowPanel(true, true) with AbstractCodeDojoViewPanel {
  private val actionManager   = ActionManager.getInstance()
  private val actionGroup     = actionManager.getAction(HACKERRANK_TOOLBAR_GROUP).asInstanceOf[ActionGroup]
  private val myActionToolbar = actionManager.createActionToolbar(TOOLBAR_PLACE, actionGroup, true)
  
  private val myQueryFiltersActionGroup = DefaultActionGroup()

  Disposer.register(myPresenter, this)

  setToolbar(myActionToolbar.getComponent)
  myActionToolbar.setTargetComponent(this)
  setContent(JBLabel("HackerRank"))

  override def uiDataSnapshot(dataSink: DataSink): Unit =
    dataSink.set(LOGIN_LOGOUT_KEY, myPresenter)
    dataSink.set(LISTS_PROVIDER_KEY, myPresenter)

  override def dispose(): Unit = {}
}
