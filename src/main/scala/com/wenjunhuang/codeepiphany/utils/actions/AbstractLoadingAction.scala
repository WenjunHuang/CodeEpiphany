package com.wenjunhuang.codeepiphany.utils.actions

import icons.CodeEpiphanyIcons
import javax.swing.JComponent

import com.intellij.openapi.actionSystem.{ ActionToolbar, AnAction, Presentation }
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.util.Key
import com.intellij.ui.{ AnimatedIcon, CardLayoutPanel }
import com.intellij.ui.components.JBLabel

import com.wenjunhuang.codeepiphany.utils.actions.AbstractLoadingAction.*

abstract class AbstractLoadingAction extends AnAction with CustomComponentAction {

  protected def setLoading(presentation: Presentation, loading: Boolean): Unit = {
    presentation.putClientProperty(LOADING_KEY, loading)
  }

  override def createCustomComponent(presentation: Presentation, place: String): JComponent = {
    LoginActionComponent(presentation, place)
  }

  override def updateCustomComponent(component: JComponent, presentation: Presentation): Unit = {
    if presentation.getClientProperty(LOADING_KEY) then
      component.asInstanceOf[LoginActionComponent].select(ComponentType.LOADING, false)
    else component.asInstanceOf[LoginActionComponent].select(ComponentType.BUTTON, false)
  }

  private class LoginActionComponent(private val presentation: Presentation, private val place: String)
      extends CardLayoutPanel[ComponentType, ComponentType, JComponent] {
    override def prepare(key: ComponentType): ComponentType = key

    override def create(ui: ComponentType): JComponent = ui match
      case ComponentType.BUTTON =>
        ActionButton(AbstractLoadingAction.this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
      case ComponentType.LOADING =>
        val label = JBLabel()
        label.setIcon(CodeEpiphanyIcons.LOADING)
        label.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
        label
  }
}
object AbstractLoadingAction {
  private enum ComponentType {
    case BUTTON
    case LOADING
  }

  private val LOADING_KEY = Key.create[Boolean]("LOGIN_STATUS_KEY")

}
