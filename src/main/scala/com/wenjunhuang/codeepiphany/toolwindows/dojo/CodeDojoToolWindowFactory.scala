package com.wenjunhuang.codeepiphany.toolwindows.dojo

import cats.syntax.all.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.actionSystem.{ActionManager, AnAction}
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.wm.{ToolWindow, ToolWindowContentUiType, ToolWindowFactory}
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.{ContentManagerEvent, ContentManagerListener}

import com.wenjunhuang.codeepiphany.codeforces.ui.CodeForcesChallengesView
import com.wenjunhuang.codeepiphany.hackerrank.ui.HackerRankChallengesView
import com.wenjunhuang.codeepiphany.leetcode.ui.LeetCodeChallengesView
import com.wenjunhuang.codeepiphany.model.{Actions, CodeDojo}
import com.wenjunhuang.codeepiphany.model.Actions.TITLE_TOOLBAR_GROUP
import com.wenjunhuang.codeepiphany.utils.ToolWindowFactoryBridge

class CodeDojoToolWindowFactory extends ToolWindowFactoryBridge {
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    val contentManager = toolWindow.getContentManager
    val contentFactory = contentManager.getFactory

    toolWindow.setDefaultContentUiType(ToolWindowContentUiType.COMBO)
    toolWindow.getComponent.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")

    contentManager.addContentManagerListener(new ContentManagerListener {
      override def selectionChanged(event: ContentManagerEvent): Unit = {
        Option(event.getContent.getActions) match
          case Some(actions) =>
            toolWindow.setTitleActions((actions.getChildren(null).toList ++ createTitleActions()).asJava)
          case None =>
            toolWindow.setTitleActions(createTitleActions().asJava)
      }
    })

    val codeForcesView    = CodeForcesChallengesView(project)
    val codeForcesContent = contentFactory.createContent(codeForcesView, CodeDojo.CodeForces.show, false)
    codeForcesContent.setActions(codeForcesView.getActions, Actions.CODEFORCES_TITLE_TOOLBAR_PLACE, codeForcesView)
    contentManager.addContent(codeForcesContent)

    val leetCodeCNView    = LeetCodeChallengesView(project, CodeDojo.LeetCodeCN)
    val leetCodeCNContent = contentFactory.createContent(leetCodeCNView, CodeDojo.LeetCodeCN.show, false)
    leetCodeCNContent.setActions(leetCodeCNView.getActions, Actions.LEETCODE_TITLE_TOOLBAR_PLACE, leetCodeCNView)
    contentManager.addContent(leetCodeCNContent)

    val leetCodeView    = LeetCodeChallengesView(project, CodeDojo.LeetCode)
    val leetCodeContent = contentFactory.createContent(leetCodeView, CodeDojo.LeetCode.show, false)
    leetCodeContent.setActions(leetCodeView.getActions, Actions.LEETCODE_TITLE_TOOLBAR_PLACE, leetCodeView)
    contentManager.addContent(leetCodeContent)

    val hackerRankView    = HackerRankChallengesView(project)
    val hackerRankContent = contentFactory.createContent(hackerRankView, CodeDojo.HackerRank.show, false)
    hackerRankContent.setActions(hackerRankView.getActions, Actions.HACKERRANK_TITLE_TOOLBAR_PLACE, hackerRankView)
    contentManager.addContent(hackerRankContent)

  }

  private def createTitleActions(): List[AnAction] = List(ActionManager.getInstance().getAction(TITLE_TOOLBAR_GROUP))
}
