package com.wenjunhuang.codeepiphany.luogu.settings

import com.intellij.openapi.components.{Service, State, Storage}
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings

@Service(Array(Level.PROJECT))
@State(name = Constants.LUOGU_SETTING, storages = Array(new Storage(Constants.LUOGU_SETTING_FILE)))
final class LuoGuSettings(project: Project) extends BaseCodeDojoSettings(project) {}

object LuoGuSettings {
  def getInstance(project: Project): LuoGuSettings =
    project.getService(classOf[LuoGuSettings])
}
