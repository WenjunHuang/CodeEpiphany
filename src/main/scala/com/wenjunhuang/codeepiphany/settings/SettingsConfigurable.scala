package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.options.Configurable

import javax.swing.JComponent

abstract class SettingsConfigurable extends Configurable {
  private var currentPanel = Option.empty[SettingsPanel]

  def createPanel(): SettingsPanel

  override def createComponent(): JComponent = {
    val panel = currentPanel.getOrElse {
      val panel = createPanel()
      currentPanel = Some(panel)
      panel
    }
    panel.getRootPanel
  }

  override def isModified: Boolean = currentPanel.get.isModified

  override def apply(): Unit = currentPanel.get.apply()

  override def reset(): Unit = currentPanel.get.reset()

  override def disposeUIResources(): Unit = {
    currentPanel = None
  }
}
