package com.wenjunhuang.codeepiphany.settings.dojo

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings.*
import com.wenjunhuang.codeepiphany.utils.ConfigConverters.*

import java.util as ju
import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*

/** Base class for code dojo settings. It provides the common functionality for similar code dojo settings.
  */
abstract class BaseCodeDojoSettings(protected val myProject: Project)
    extends PersistentStateComponent[CodeDojoSettingsState] {
  private var state = CodeDojoSettingsState()

  override def getState: CodeDojoSettingsState = state

  override def loadState(newState: CodeDojoSettingsState): Unit =
    state = newState

  def getSelectedLanguages: List[(Language, LanguageVersion)] =
    state.languageSettings.asScala.toList.map { it =>
      (it.language.get, it.languageVersion.get)
    }

  def getLanguageSetting(language: Language, languageVersion: LanguageVersion): Option[LanguageSettingsState] = {
    state.languageSettings.asScala.find { state =>
      state.language.contains(language) && state.languageVersion.contains(languageVersion)
    }
  }
}

object BaseCodeDojoSettings {
  class CodeDojoSettingsState {
    @BeanProperty
    var languageSettings: ju.List[LanguageSettingsState] = new ju.ArrayList[LanguageSettingsState]()
    @BeanProperty
    var queryCriteria: ju.Map[String, String] = new ju.HashMap[String, String]()
  }

  class LanguageSettingsState {

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
}
