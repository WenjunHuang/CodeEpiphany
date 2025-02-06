package com.wenjunhuang.codeepiphany.codeforces.settings

import com.intellij.openapi.components.{Service, State, Storage}
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings

@Service(Array(Level.PROJECT))
@State(name = Constants.CODEFORCES_SETTING, storages = Array(new Storage(Constants.CODEFORCES_SETTING_FILE)))
final class CodeForcesSettings(project: Project) extends BaseCodeDojoSettings(project) {}

object CodeForcesSettings {
  def getInstance(project: Project): CodeForcesSettings =
    project.getService(classOf[CodeForcesSettings])
}
