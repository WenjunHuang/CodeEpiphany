package com.wenjunhuang.codeepiphany.leetcode.settings

import com.intellij.openapi.components.{Service, State, Storage}
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings

@Service(Array(Level.PROJECT))
@State(name = Constants.LEETCODE_SETTING, storages = Array(new Storage(Constants.LEETCODE_SETTING_FILE)))
final class LeetCodeSettings(project: Project) extends BaseCodeDojoSettings(project) {}

object LeetCodeSettings {
  def getInstance(project: Project): LeetCodeSettings =
    project.getService(classOf[LeetCodeSettings])
}
