package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.{ PersistentStateComponent, Service, State, Storage }
import com.intellij.util.xmlb.annotations.OptionTag
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Constants }
import com.wenjunhuang.codeepiphany.utils.XmlUtils.CodeDojoConverter

import scala.beans.BeanProperty
import java.util as ju
import scala.annotation.meta.{ beanGetter, beanSetter }
import scala.compiletime.uninitialized
import ChallengeSettings.ChallengeSettingsState
import com.intellij.openapi.project.Project

@Service(Array(Level.PROJECT))
@State(name = Constants.CHALLENGE_SETTING, storages = Array(new Storage(Constants.CHALLENGE_SETTING_FILE)))
final class ChallengeSettings extends PersistentStateComponent[ChallengeSettingsState] {
  private var myState                           = ChallengeSettingsState()
  override def getState: ChallengeSettingsState = myState

  override def loadState(state: ChallengeSettingsState): Unit =
    myState = state
}

object ChallengeSettings {

  def getInstance(project: Project): ChallengeSettings = project.getService(classOf[ChallengeSettings])

  class ChallengeSettingsState {
    @BeanProperty
    var challenges: ju.Map[String, Integer] = new ju.HashMap[String, Integer]()

  }
}
