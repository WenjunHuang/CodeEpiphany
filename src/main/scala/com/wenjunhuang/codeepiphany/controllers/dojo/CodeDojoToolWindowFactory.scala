package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.IO
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionManager, AnAction, DataProvider}
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.openapi.wm.{ToolWindow, ToolWindowContentUiType, ToolWindowFactory}
import com.intellij.ui.components.JBLabel
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.groups.TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.LISTS_PROVIDER_KEY

import scala.jdk.CollectionConverters.*
import javax.swing.Icon

class CodeDojoToolWindowFactory extends ToolWindowFactory with DumbAware {
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val contentManager = toolWindow.getContentManager
    val contentFactory = contentManager.getFactory

    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
    toolWindow.setDefaultContentUiType(ToolWindowContentUiType.COMBO)

    contentManager.addContent(contentFactory.createContent(HackerRankPanel(project), "HackerRank", false))
    contentManager.addContent(contentFactory.createContent(JBLabel("Leetcode"), "Leetcode", false))
    contentManager.addContent(contentFactory.createContent(JBLabel("CodeWars"), "CodeWars", false))

    val titleActions = createTitleActions()
    if titleActions.nonEmpty then 
      toolWindow.setTitleActions(titleActions.asJava)
  }

  private def createTitleActions(): List[AnAction] = List(ActionManager.getInstance().getAction(TITLE_TOOLBAR_GROUP))

  override def getIcon: Icon = AllIcons.General.Alpha
}
