package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.{ PersistentStateComponent, Service, State, Storage }
import com.intellij.util.xmlb.annotations.OptionTag
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Constants, Language }
import com.wenjunhuang.codeepiphany.utils.XmlUtils.{ CodeDojoConverter, IntOptionConverter, LanguageConverter }

import scala.beans.BeanProperty
import java.util as ju
import scala.annotation.meta.{ beanGetter, beanSetter }
import scala.compiletime.uninitialized
import ChallengeSettings.{ ChallengeSettingsState, ChallengeSettingsStateItem }
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.{ ChallengeId, ChallengeLanguageId, SolutionId }

@Service(Array(Level.PROJECT))
@State(name = Constants.CHALLENGE_SETTING, storages = Array(new Storage(Constants.CHALLENGE_SETTING_FILE)))
final class ChallengeSettings extends PersistentStateComponent[ChallengeSettingsState] {
  private var myState                           = ChallengeSettingsState()
  override def getState: ChallengeSettingsState = myState

  override def loadState(state: ChallengeSettingsState): Unit =
    myState = state

  def addChallenge(key: String, item: ChallengeSettingsStateItem): Unit = synchronized {
    val newItem = new ChallengeSettingsStateItem()
    newItem.challengeId = item.challengeId
    newItem.challengeLanguageId = item.challengeLanguageId
    newItem.solutionId = item.solutionId
    myState.challenges.put(key, newItem)
  }

  def addChallenge(vf: VirtualFile, item: ChallengeSettingsStateItem): Unit = synchronized {
    addChallenge(vf.getCanonicalPath, item)
  }

  def findChallengeId(filePath: String): Option[ChallengeSettingsStateItem] = synchronized {
    Option(myState.challenges.get(filePath)).map { item =>
      val result = new ChallengeSettingsStateItem()
      result.challengeId = item.challengeId
      result.challengeLanguageId = item.challengeLanguageId
      result.solutionId = item.solutionId
      result
    }
  }

  def findChallengeId(vf: VirtualFile): Option[ChallengeSettingsStateItem] =
    findChallengeId(vf.getCanonicalPath)
}

object ChallengeSettings {

  def getInstance(project: Project): ChallengeSettings = project.getService(classOf[ChallengeSettings])

  class ChallengeSettingsStateItem {
    @BeanProperty
    var challengeId: Int = uninitialized
    
    @BeanProperty
    var challengeLanguageId: Int = uninitialized

    @(OptionTag @beanGetter @beanSetter)(converter = classOf[CodeDojoConverter])
    @BeanProperty
    var dojo: CodeDojo = uninitialized

    @(OptionTag @beanGetter @beanSetter)(converter = classOf[LanguageConverter])
    @BeanProperty
    var language: Language = uninitialized

    @(OptionTag @beanGetter @beanSetter)(converter = classOf[IntOptionConverter])
    @BeanProperty
    var solutionId: Option[Int] = None
  }

  object ChallengeSettingsStateItem {
    def apply(
      challengeId: ChallengeId,
      challengeLanguageId: ChallengeLanguageId,
      codeDojo: CodeDojo,
      language: Language,
      solutionId: Option[SolutionId]
    ): ChallengeSettingsStateItem = {
      val r = new ChallengeSettingsStateItem()
      r.challengeId = challengeId.value
      r.challengeLanguageId = challengeLanguageId.value
      r.dojo = codeDojo
      r.language = language
      r.solutionId = solutionId.map(_.value)
      r
    }
  }

  class ChallengeSettingsState {
    @BeanProperty
    var challenges: ju.Map[String, ChallengeSettingsStateItem] = new ju.HashMap[String, ChallengeSettingsStateItem]()

  }
}
