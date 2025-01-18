package com.wenjunhuang.codeepiphany.hackerrank.settings

import com.intellij.openapi.components.{Service, State, Storage}
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings

@Service(Array(Level.PROJECT))
@State(name = Constants.HACKERRANK_SETTING, storages = Array(new Storage(Constants.HACKERRANK_SETTING_FILE)))
final class HackerRankSettings(project: Project) extends BaseCodeDojoSettings(project) {
  
}

object HackerRankSettings {
  def getInstance(project: Project): HackerRankSettings =
    project.getService(classOf[HackerRankSettings])
}
