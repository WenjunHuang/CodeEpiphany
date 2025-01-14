package com.wenjunhuang.codeepiphany.hackerrank.settings

import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.{PersistentStateComponent, Service, State, Storage}
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings.{HackerRankLanguageSettingsState, HackerRankSettingsState}
import com.wenjunhuang.codeepiphany.model.{Constants, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.utils.XmlUtils.*

import java.util as ju
import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*

@Service(Array(Level.PROJECT))
@State(name = Constants.HACKERRANK_SETTING, storages = Array(new Storage(Constants.HACKERRANK_SETTING_FILE)))
final class HackerRankSettings(private val myProject: Project)
    extends PersistentStateComponent[HackerRankSettingsState] {
  private var state = HackerRankSettingsState()

  override def getState: HackerRankSettingsState = state

  override def loadState(newState: HackerRankSettingsState): Unit =
    state = newState

  def getSelectedLanguages: List[(Language, LanguageVersion)] =
    state.languageSettings.asScala.toList.map { it =>
      (it.language.get, it.languageVersion.get)
    }

  def getLanguageSetting(
    language: Language,
    languageVersion: LanguageVersion
  ): Option[HackerRankLanguageSettingsState] = {
    state.languageSettings.asScala.find { state =>
      state.language.contains(language) && state.languageVersion.contains(languageVersion)
    }
  }
}

object HackerRankSettings {
  class HackerRankSettingsState {
    @BeanProperty
    var languageSettings: ju.List[HackerRankLanguageSettingsState] = new ju.ArrayList[HackerRankLanguageSettingsState]()
  }

  class HackerRankLanguageSettingsState {
    
    @(Attribute @field)(converter = classOf[StringOptionConverter])
    var sourceFolder: Option[String] = None

    @(Attribute @field)(converter = classOf[LanguageOptionConverter])
    @BeanProperty
    var language: Option[Language] = None

    @(Attribute @field)(converter = classOf[LanguageVersionOptionConverter])
    @BeanProperty
    var languageVersion: Option[LanguageVersion] = None

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
