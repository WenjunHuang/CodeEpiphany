package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.IO
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionManager, AnAction, DataProvider}
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.openapi.wm.{ToolWindow, ToolWindowContentUiType, ToolWindowFactory}
import com.intellij.ui.components.JBLabel
import com.wenjunhuang.codeepiphany.controllers.dojo.CodeDojoToolWindowFactory.HACKERRANK_PRESENTER_KEY
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

    val hackerRankPresenter = HackerRankPresenter(project)
    val content             = contentFactory.createContent(hackerRankPresenter.getComponent(), "HackerRank", false)
    content.putUserData(HACKERRANK_PRESENTER_KEY, hackerRankPresenter)
    contentManager.addContent(content)
    contentManager.addContent(contentFactory.createContent(JBLabel("Leetcode"), "Leetcode", false))
    contentManager.addContent(contentFactory.createContent(JBLabel("CodeWars"), "CodeWars", false))

    val titleActions = createTitleActions()
    if titleActions.nonEmpty then toolWindow.setTitleActions(titleActions.asJava)
  }

  private def createTitleActions(): List[AnAction] = List(ActionManager.getInstance().getAction(TITLE_TOOLBAR_GROUP))

  override def getIcon: Icon = AllIcons.General.Alpha
}

object CodeDojoToolWindowFactory {
  val HACKERRANK_PRESENTER_KEY: Key[HackerRankPresenter] = Key.create[HackerRankPresenter]("HACKERRANK_PRESENTER_KEY")
}
