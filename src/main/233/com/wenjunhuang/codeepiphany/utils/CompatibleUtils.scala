package com.wenjunhuang.codeepiphany.utils

import java.net.{PasswordAuthentication, ProxySelector, URL}

import com.intellij.openapi.actionSystem.{ActionGroup, ActionManager, ActionToolbar, AnAction}
import com.intellij.util.net.*

object CompatibleUtils {
  inline def setToolBarWrapLayout(toolBar: ActionToolbar): Unit = {
    toolBar.setLayoutPolicy(ActionToolbar.WRAP_LAYOUT_POLICY)
  }

  inline def updateActions(toolBar: ActionToolbar): Unit = {
    toolBar.updateActionsImmediately()
  }

  inline def getIdeaProxyPasswordAuthentication(url: URL): PasswordAuthentication = {
    val httpConfigurable = HttpConfigurable.getInstance()
    val ideaAuthenticator = IdeaWideAuthenticator(httpConfigurable)
    ideaAuthenticator.getPasswordAuthentication
  }

  inline def getIdeaProxySelector: ProxySelector = {
    val httpConfigurable = HttpConfigurable.getInstance()
    val ideaProxySelector = IdeaWideProxySelector(httpConfigurable) // IntelliJ proxy selector
    ideaProxySelector
  }
  inline def getActionGroupChildren(actionGroup: ActionGroup): List[AnAction] =
    actionGroup.getChildren(null, ActionManager.getInstance()).toList
}
