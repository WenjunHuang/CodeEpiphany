package com.wenjunhuang.codeepiphany.utils

import com.intellij.credentialStore.Credentials
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.util.net.*

import java.net.*


object CompatibleUtils {
  inline def setToolBarWrapLayout(toolBar: ActionToolbar): Unit = {
    toolBar.setLayoutStrategy(ToolbarLayoutStrategy.WRAP_STRATEGY)
  }

  inline def updateActions(toolBar: ActionToolbar): Unit = {
    toolBar.updateActionsAsync()
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
