package com.wenjunhuang.codeepiphany.toolwindows.dojo

import cats.syntax.all.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.actionSystem.{ActionManager, AnAction}
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.wm.{ToolWindow, ToolWindowContentUiType, ToolWindowManager}
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.{ContentManager, ContentManagerEvent, ContentManagerListener}

import com.wenjunhuang.codeepiphany.atcoder.ui.AtCoderChallengesView
import com.wenjunhuang.codeepiphany.codeforces.ui.CodeForcesChallengesView
import com.wenjunhuang.codeepiphany.hackerrank.ui.HackerRankChallengesView
import com.wenjunhuang.codeepiphany.leetcode.ui.LeetCodeChallengesView
import com.wenjunhuang.codeepiphany.model.{Actions, CodeDojo}
import com.wenjunhuang.codeepiphany.model.Actions.TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettings
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettings
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.leetcode.settings.{LeetCodeCNSettings, LeetCodeSettings}
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettings
import com.wenjunhuang.codeepiphany.luogu.ui.LuoGuChallengesView
import com.wenjunhuang.codeepiphany.services.BaseChallengesView
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings
import com.wenjunhuang.codeepiphany.settings.CodeEpiphanySettings
import com.wenjunhuang.codeepiphany.settings.CodeEpiphanySettings.CodeEpiphanySettingsChangedListener
import com.wenjunhuang.codeepiphany.toolwindows.dojo.CodeDojoToolWindowFactory.updateContents
import com.wenjunhuang.codeepiphany.utils.walkaround.ToolWindowFactoryBridge

class CodeDojoToolWindowFactory extends ToolWindowFactoryBridge with DumbAware {
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val contentManager = toolWindow.getContentManager

    toolWindow.setDefaultContentUiType(ToolWindowContentUiType.COMBO)
    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")

    contentManager.addContentManagerListener(new ContentManagerListener {
      override def selectionChanged(event: ContentManagerEvent): Unit = {
        Option(event.getContent.getActions) match
          case Some(actions) =>
            toolWindow.setTitleActions(
              (actions.getChildren(null, ActionManager.getInstance()).toList ++ createTitleActions()).asJava
            )
          case None =>
            toolWindow.setTitleActions(createTitleActions().asJava)
      }
    })

    project.getMessageBus
      .connect(project)
      .subscribe(
        CodeEpiphanySettings.TOPIC,
        new CodeEpiphanySettingsChangedListener {
          override def changed(): Unit = {
            updateContents(project, toolWindow)
          }
        }
      )

    updateContents(project, toolWindow)
  }

  private def createTitleActions(): List[AnAction] = List(ActionManager.getInstance().getAction(TITLE_TOOLBAR_GROUP))
}

object CodeDojoToolWindowFactory {
  final val TOOL_WINDOW_ID = "CodeEpiphany CodeDojos"

  def getToolWindow(project: Project): ToolWindow = {
    ToolWindowManager.getInstance(project).getToolWindow(CodeDojoToolWindowFactory.TOOL_WINDOW_ID) match
      case null => throw IllegalStateException(s"Could not find tool window for id ${TOOL_WINDOW_ID}")
      case tw   => tw
  }

  private def updateContents(project: Project, toolWindow: ToolWindow): Unit = {
    val contentManager = toolWindow.getContentManager
    val contentFactory = contentManager.getFactory

    Option(contentManager.findContent(PluginBundle.message("ui.setupView.title")))
      .foreach(contentManager.removeContent(_, true))

    updateCodeDojoContent(
      project,
      contentManager,
      CodeDojo.AtCoder,
      classOf[AtCoderSettings],
      AtCoderChallengesView(project)
    )
    updateCodeDojoContent(
      project,
      contentManager,
      CodeDojo.CodeForces,
      classOf[CodeForcesSettings],
      CodeForcesChallengesView(project)
    )
    updateCodeDojoContent(
      project,
      contentManager,
      CodeDojo.LeetCodeCN,
      classOf[LeetCodeCNSettings],
      LeetCodeChallengesView(project, CodeDojo.LeetCodeCN)
    )
    updateCodeDojoContent(
      project,
      contentManager,
      CodeDojo.LeetCode,
      classOf[LeetCodeSettings],
      LeetCodeChallengesView(project, CodeDojo.LeetCode)
    )
    updateCodeDojoContent(
      project,
      contentManager,
      CodeDojo.HackerRank,
      classOf[HackerRankSettings],
      HackerRankChallengesView(project)
    )
    updateCodeDojoContent(project,
      contentManager,
      CodeDojo.LuoGu,
      classOf[LuoGuSettings],
      LuoGuChallengesView(project))

    if contentManager.getContentCount == 0 then
      val setupRequiredPromptView = SetupRequiredPromptView(project)
      val setupContent = contentFactory.createContent(
        setupRequiredPromptView.getComponent,
        PluginBundle.message("ui.setupView.title"),
        false
      )
      contentManager.addContent(setupContent)
  }

  private def updateCodeDojoContent(
    project: Project,
    contentManager: ContentManager,
    codeDojo: CodeDojo,
    classOf: Class[? <: BaseCodeDojoSettings],
    viewCreator: => BaseChallengesView[?]
  ): Unit = {
    val contentFactory = contentManager.getFactory
    if project.getService(classOf).getSelectedLanguages.nonEmpty then
      if contentManager.findContent(codeDojo.show) == null then
        val view    = viewCreator
        val content = contentFactory.createContent(view, codeDojo.show, false)
        content.setActions(view.getTitleActionGroup, Actions.TITLE_TOOLBAR_PLACE, view)
        contentManager.addContent(content)
    else
      contentManager.findContent(codeDojo.show) match
        case null    =>
        case content => contentManager.removeContent(content, true)
  }
}
