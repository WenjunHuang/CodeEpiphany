package com.wenjunhuang.codeepiphany.toolwindows.dojo

import cats.syntax.all.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.actionSystem.{ActionManager, AnAction}
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.wm.{ToolWindow, ToolWindowContentUiType, ToolWindowFactory}
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.components.JBLabel

import com.wenjunhuang.codeepiphany.model.Actions.TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.HackerRankView

class CodeDojoToolWindowFactory extends ToolWindowFactory with DumbAware {
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val contentManager = toolWindow.getContentManager
    val contentFactory = contentManager.getFactory

    toolWindow.setDefaultContentUiType(ToolWindowContentUiType.COMBO)
    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")

    val hackerRankView = HackerRankView(project)
    val content        = contentFactory.createContent(hackerRankView, CodeDojo.HackerRank.show, false)
    contentManager.addContent(content)
    contentManager.addContent(contentFactory.createContent(JBLabel("LeetcodeCN"), CodeDojo.LeetCodeCN.show, false))
    contentManager.addContent(contentFactory.createContent(JBLabel("LeetCode"), CodeDojo.LeetCode.show, false))

    val titleActions = createTitleActions()
    if titleActions.nonEmpty then toolWindow.setTitleActions(titleActions.asJava)
  }

  private def createTitleActions(): List[AnAction] = List(ActionManager.getInstance().getAction(TITLE_TOOLBAR_GROUP))
}
