package com.wenjunhuang.codeepiphany.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.CardLayoutPanel
import com.wenjunhuang.codeepiphany.utils.actions.UiDataProvider

import javax.swing.JComponent

abstract class BaseChallengesView[UI]
    extends CardLayoutPanel[UI, UI, JComponent]
    with UiDataProvider
    with DumbAware
    with Disposable {
  
  def getTitleActionGroup: ActionGroup
  
  override def prepare(key: UI): UI = key
}
