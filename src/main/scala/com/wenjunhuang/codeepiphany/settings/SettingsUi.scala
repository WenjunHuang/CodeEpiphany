package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ConfigurableUi
import com.intellij.openapi.project.Project

abstract class SettingsUi[S](protected val myProject: Project) extends ConfigurableUi[S] with Disposable {
  override def dispose(): Unit = {}
}
