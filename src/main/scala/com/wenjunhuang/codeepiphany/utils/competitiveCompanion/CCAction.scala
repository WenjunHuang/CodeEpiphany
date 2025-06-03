package com.wenjunhuang.codeepiphany.utils.competitiveCompanion

import com.intellij.openapi.actionSystem.ex.CheckboxAction
import com.intellij.openapi.actionSystem.{ AnActionEvent, DataKey }
import com.wenjunhuang.codeepiphany.utils.actions.{ ActionCompatible, DataKeyNotNull }
import com.wenjunhuang.codeepiphany.utils.competitiveCompanion.CCAction.CC_ACTION_PROVIDER_KEY

abstract class CCAction extends CheckboxAction with ActionCompatible with DataKeyNotNull(CC_ACTION_PROVIDER_KEY) {

  override def isSelected(e: AnActionEvent): Boolean = {
    getValue(e).isListening
  }

  override def setSelected(e: AnActionEvent, state: Boolean): Unit = {
    val provider = getValue(e)
    if state then provider.startListening()
    else provider.stopListening()
  }

  override def update(e: AnActionEvent): Unit = {
    super.update(e)
    val presentation = e.getPresentation
    if isSatisfied(e) then presentation.setEnabledAndVisible(true)
    else presentation.setEnabledAndVisible(false)

  }
}

object CCAction {
  val CC_ACTION_PROVIDER_KEY: DataKey[CCActionProvider] = DataKey.create[CCActionProvider]("CC_ACTION_PROVIDER_KEY")

  trait CCActionProvider {
    def startListening(): Unit

    def stopListening(): Unit

    def isListening: Boolean
  }
}
