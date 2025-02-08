package com.wenjunhuang.codeepiphany.atcoder.settings

import com.intellij.openapi.components.{ Service, State, Storage }
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings

@Service(Array(Level.PROJECT))
@State(name = Constants.ATCODER_SETTING, storages = Array(new Storage(Constants.ATCODER_SETTING_FILE)))
final class AtCodeSettings(project: Project) extends BaseCodeDojoSettings(project) {}

object AtCodeSettings {
  def getInstance(project: Project): AtCodeSettings = project.getService(classOf[AtCodeSettings])
}
