package com.wenjunhuang.codeepiphany.hackerrank

import com.intellij.openapi.components.{PersistentStateComponent, Service, State, Storage}
import com.wenjunhuang.codeepiphany.model.Constants
import HackerRankSettings.*
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project

@Service(Array(Level.PROJECT))
@State(name = Constants.HACKERRANK_SETTING, storages = Array(new Storage(Constants.HACKERRANK_SETTING_FILE)))
final class HackerRankSettings(private val myProject:Project) extends PersistentStateComponent[HackerRankState] {
  private var state = HackerRankState()

  override def getState: HackerRankState = state

  override def loadState(newState: HackerRankState): Unit = state = newState
}

object HackerRankSettings {
  class HackerRankState {
    var sourceFolder: String            = ""
    var language: String                = ""
    var languageVersion: String         = ""
    var codeFileNameTemplate: String    = ""
    var codeFileContentTemplate: String = ""
  }
  
  def getInstance(project:Project): HackerRankSettings = project.getService(classOf[HackerRankSettings])
}
