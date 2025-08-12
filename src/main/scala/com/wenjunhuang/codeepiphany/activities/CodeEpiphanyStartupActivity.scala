package com.wenjunhuang.codeepiphany.activities

import kotlin.coroutines.Continuation

import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.startup.ProjectActivity

import com.wenjunhuang.codeepiphany.notifications.CodeEpiphanyNotification
import com.wenjunhuang.codeepiphany.settings.CodeEpiphanyAppSettings
import com.wenjunhuang.codeepiphany.utils.IdeUtils

class CodeEpiphanyStartupActivity extends ProjectActivity with DumbAware{

  override def execute(project: Project, continuation: Continuation[? >: kotlin.Unit]): AnyRef = {
    val settings = CodeEpiphanyAppSettings.getInstance.getState
    settings.version match {
      case None=>
        settings.version = Some(IdeUtils.pluginVersion)
        CodeEpiphanyNotification.notifyFirstlyDownloaded(project)
      case Some(version) =>
        if (version != IdeUtils.pluginVersion) {
          settings.version = Some(IdeUtils.pluginVersion)
            CodeEpiphanyNotification.notifyReleaseNote(project)
        }
    }
    ""
  }
}
