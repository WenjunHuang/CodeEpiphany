package com.wenjunhuang.codeepiphany.hackerrank.settings

import com.intellij.openapi.components.{ PersistentStateComponent, Service, State, Storage }
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.{ Attribute, OptionTag }
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings.HackerRankState
import com.wenjunhuang.codeepiphany.model.{ Constants, Language }
import com.wenjunhuang.codeepiphany.utils.XmlUtils.*

import scala.annotation.meta.{ beanGetter, beanSetter, field }
import scala.beans.BeanProperty
import java.util as ju

@Service(Array(Level.PROJECT))
@State(name = Constants.HACKERRANK_SETTING, storages = Array(new Storage(Constants.HACKERRANK_SETTING_FILE)))
final class HackerRankSettings(private val myProject: Project) extends PersistentStateComponent[HackerRankState] {
  private var state = HackerRankState()

  override def getState: HackerRankState = state

  override def loadState(newState: HackerRankState): Unit =
    state = newState

  def getSelectedLanguages: List[Language] = List(Language.Java, Language.Kotlin)
}

object HackerRankSettings {

  class HackerRankState {
    @(Attribute @field)(converter = classOf[StringOptionConverter])
    var sourceFolder: Option[String] = None

    @(Attribute @field)(converter = classOf[LanguageOptionConverter])
    @BeanProperty
    var language: Option[Language] = None

    @(Attribute @field)(converter = classOf[StringOptionConverter])
    @BeanProperty
    var fileNameTemplate: Option[String] = None

    @(Attribute @field)(converter = classOf[StringOptionConverter])
    @BeanProperty
    var codeTemplate: Option[String] = None
  }

  def getInstance(project: Project): HackerRankSettings =
    project.getService(classOf[HackerRankSettings])
}
