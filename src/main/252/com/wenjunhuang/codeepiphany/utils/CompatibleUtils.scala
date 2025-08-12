package com.wenjunhuang.codeepiphany.utils

import java.net.*

import com.intellij.openapi.actionSystem.*
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
    ProxyUtils.getStaticProxyCredentials(
      ProxySettings.getInstance(),
      ProxyCredentialStoreKt.asProxyCredentialProvider(ProxyCredentialStore.getInstance())
    ) match {
      case null => null
      case p    => PasswordAuthentication(p.getUserName, p.getPassword.toCharArray)
    }
  }

  inline def getIdeaProxySelector: ProxySelector = {
    val ideaProxySelector = IdeProxySelector(
      ProxySettingsKt.asConfigurationProvider(ProxySettings.getInstance())
    ) // IntelliJ proxy selector
    ideaProxySelector
  }

  inline def getActionGroupChildren(actionGroup: ActionGroup): List[AnAction] = {
    actionGroup match {
      case dag: DefaultActionGroup =>
        dag.getChildren(ActionManager.getInstance()).toList
      case _ => Nil
    }
  }
}
