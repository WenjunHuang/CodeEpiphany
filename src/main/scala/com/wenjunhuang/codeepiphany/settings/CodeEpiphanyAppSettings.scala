package com.wenjunhuang.codeepiphany.settings

import scala.annotation.meta.field

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.components.Service.Level
import com.intellij.util.xmlb.annotations.Attribute

import com.wenjunhuang.codeepiphany.model.Constants
import com.wenjunhuang.codeepiphany.utils.XmlUtils.StringOptionConverter

@Service(Array(Level.APP))
@State(name = Constants.APP_SETTING, storages = Array(new Storage(Constants.APP_SETTING_FILE)) )
final class CodeEpiphanyAppSettings extends PersistentStateComponent[CodeEpiphanyAppSettings.State]() {
  private var state = CodeEpiphanyAppSettings.State()

  override def getState: CodeEpiphanyAppSettings.State = state

  override def loadState(newState: CodeEpiphanyAppSettings.State): Unit =
    state = newState
}

object CodeEpiphanyAppSettings {
  class State {
    @(Attribute @field)(converter = classOf[StringOptionConverter])
    var version: Option[String] = None
  }

  def getInstance:CodeEpiphanyAppSettings = {
    ApplicationManager.getApplication.getService(classOf[CodeEpiphanyAppSettings])
  }
}
