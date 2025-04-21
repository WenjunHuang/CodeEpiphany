package com.wenjunhuang.codeepiphany.utils

import com.intellij.openapi.actionSystem.ActionToolbar

object CompatibleUtils {
  inline def setToolBarWrapLayout(toolBar: ActionToolbar): Unit = {
    toolBar.setLayoutPolicy(ActionToolbar.WRAP_LAYOUT_POLICY)
  }

  inline def updateActions(toolBar: ActionToolbar): Unit = {
    toolBar.updateActionsImmediately()
  }

  inline def getIdeaProxyPasswordAuthentication(url: URL) = {
    val httpConfigurable = HttpConfigurable.getInstance()
    val ideaAuthenticator = IdeaWideAuthenticator(httpConfigurable)
    ideaAuthenticator.getPasswordAuthentication
  }

  inline def getIdeaProxySelector: ProxySelector = {
    val httpConfigurable = HttpConfigurable.getInstance()
    val ideaAuthenticator = IdeaWideAuthenticator(httpConfigurable)
    val ideaProxySelector = IdeaWideProxySelector(httpConfigurable) // IntelliJ proxy selector
    ideaProxySelector
  }
}
