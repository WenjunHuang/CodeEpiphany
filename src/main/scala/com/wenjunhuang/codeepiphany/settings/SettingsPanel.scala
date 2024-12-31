package com.wenjunhuang.codeepiphany.settings

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.Disposable

import javax.swing.JComponent

abstract class SettingsPanel(protected val myProject:Project) {
  protected val myDisposable = Disposer.newDisposable(getClass.getName)

  def getDisposable: Disposable = myDisposable

  def getRootPanel:JComponent

  def isModified:Boolean

  @throws[ConfigurationException]
  def apply():Unit

  def reset():Unit



}
