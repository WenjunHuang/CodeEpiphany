package com.wenjunhuang.codeepiphany.settings.dojo

import java.util as ju
import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettings
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettings
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.leetcode.settings.{ LeetCodeCNSettings, LeetCodeSettings }
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettings
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings.*
import com.wenjunhuang.codeepiphany.utils.ConfigConverters.*

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

    @BeanProperty
    var descriptionCSS: String = ""

    @BeanProperty
    var extras: ju.Map[String, String] = new ju.HashMap[String, String]()
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

  def getInstance(project: Project, codeDojo: CodeDojo): BaseCodeDojoSettings = {
    codeDojo match {
      case CodeDojo.LeetCode   => LeetCodeSettings.getInstance(project)
      case CodeDojo.LeetCodeCN => LeetCodeCNSettings.getInstance(project)
      case CodeDojo.CodeForces => CodeForcesSettings.getInstance(project)
      case CodeDojo.AtCoder    => AtCoderSettings.getInstance(project)
      case CodeDojo.LuoGu      => LuoGuSettings.getInstance(project)
      case CodeDojo.HackerRank => HackerRankSettings.getInstance(project)
    }
  }
}
