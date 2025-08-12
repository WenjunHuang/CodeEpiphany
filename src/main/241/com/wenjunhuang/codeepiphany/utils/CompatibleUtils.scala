package com.wenjunhuang.codeepiphany.utils

import java.net.*

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, ActionToolbar, AnAction}
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.util.net.*

object CompatibleUtils {
  inline def setToolBarWrapLayout(toolBar: ActionToolbar): Unit = {
    toolBar.setLayoutStrategy(ToolbarLayoutStrategy.WRAP_STRATEGY)
  }

  inline def updateActions(toolBar: ActionToolbar): Unit = {
    toolBar.updateActionsAsync()
  }

  inline def getIdeaProxyPasswordAuthentication(url: URL): PasswordAuthentication = {
    val httpConfigurable  = HttpConfigurable.getInstance()
    (httpConfigurable.getProxyLogin,httpConfigurable.getPlainProxyPassword) match {
      case (null, null) => null
      case (login, password) =>
        PasswordAuthentication(login, password.toCharArray)
    }
  }

  inline def getIdeaProxySelector: ProxySelector = {
    val httpConfigurable  = HttpConfigurable.getInstance()
    val ideaProxySelector = IdeaWideProxySelector(httpConfigurable) // IntelliJ proxy selector
    ideaProxySelector
  }

  inline def getActionGroupChildren(actionGroup: ActionGroup): List[AnAction] =
    actionGroup.getChildren(null, ActionManager.getInstance()).toList
}
