package com.wenjunhuang.codeepiphany.settings.dojo

import com.intellij.openapi.options.ConfigurableBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}

import scala.jdk.CollectionConverters.*

abstract class BaseSettingsConfigurable(
  protected val myProject: Project,
  protected val myCodeDojo: CodeDojo,
  id: String,
  displayName: String,
  helpTopic: String
) extends ConfigurableBase[SettingsTabPanel, BaseCodeDojoSettings.CodeDojoSettingsState](
      id,
      displayName,
      helpTopic
    ) {

  private val myDisposable = Disposer.newDisposable(s"${myCodeDojo.value}SettingsConfigurable")


  override def disposeUIResources(): Unit = {
    Disposer.dispose(myDisposable)
  }

  def supportedLanguages: List[(Language, LanguageVersion)]

  def createDemoTemplate(language: Language, languageVersion: LanguageVersion): Option[Any]

  override def createUi(): SettingsTabPanel =
    SettingsTabPanel(
      myProject,
      myCodeDojo,
      supportedLanguages,
      { (language, languageVersion) => createDemoTemplate(language, languageVersion).get },
      myDisposable
    )
}
