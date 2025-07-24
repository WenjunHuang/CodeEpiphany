package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.options.ConfigurableBase
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.PluginBundle

class CodeEpiphanySettingsConfigurable(private val myProject: Project)
    extends ConfigurableBase[CodeEpiphanySettingsPanel, CodeEpiphanySettings.CodeEpiphanySettingsState](
      "CodeEpiphany.Settings",
      PluginBundle.message("settings.displayName"),
      "CodeEpiphany.Settings.HelpTopic"
    ) {
  override def getSettings: CodeEpiphanySettings.CodeEpiphanySettingsState =
    CodeEpiphanySettings.getInstance(myProject).getState

  override def createUi(): CodeEpiphanySettingsPanel = new CodeEpiphanySettingsPanel(myProject)
  
}
