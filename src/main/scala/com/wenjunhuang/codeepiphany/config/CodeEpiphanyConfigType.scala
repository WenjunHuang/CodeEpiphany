package com.wenjunhuang.codeepiphany.config

import icons.CodeEpiphanyIcons

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationTypeBase, RunConfiguration}
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.config.CodeEpiphanyConfigType.*

class CodeEpiphanyConfigType extends ConfigurationTypeBase(ID, NAME, DESCRIPTION, CodeEpiphanyIcons.PLUGIN) {
    addFactory(new CodeEpiphanyConfigFactory(this))
}

class CodeEpiphanyConfigFactory(configType: CodeEpiphanyConfigType) extends ConfigurationFactory(configType) {
  override def createTemplateConfiguration(project: Project): RunConfiguration = ???
}

object CodeEpiphanyConfigType {
  val ID          = "CodeEpiphanyRunConfiguration"
  val NAME        = "Code Epiphany"
  val DESCRIPTION = "Run configuration for Code Epiphany challenges"

}
