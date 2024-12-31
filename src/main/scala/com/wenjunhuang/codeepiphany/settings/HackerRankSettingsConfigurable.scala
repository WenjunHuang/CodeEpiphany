package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.PluginBundle

class HackerRankSettingsConfigurable(private val myProject: Project)
    extends SettingsConfigurable {
  override def createPanel(): SettingsPanel = new HackerRankSettingsPanel(myProject)

  override def getDisplayName: String =
    PluginBundle.message("hackerrank.settings.title")
}
