package com.wenjunhuang.codeepiphany.utils

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.util.net.*

import java.net.{PasswordAuthentication, ProxySelector, URL}

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
}
