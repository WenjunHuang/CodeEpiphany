package com.wenjunhuang.codeepiphany.actions

import icons.CodeEpiphanyIcons
import javax.swing.JComponent

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.util.Key
import com.intellij.ui.{AnimatedIcon, CardLayoutPanel}
import com.intellij.ui.components.JBLabel

import com.wenjunhuang.codeepiphany.actions.LoginAction.*

class LoginAction extends AnAction with CustomComponentAction {

  override def actionPerformed(e: AnActionEvent): Unit =
    Option(LOGIN_LOGOUT_KEY.getData(e.getDataContext)).foreach(_.login())

  override def update(e: AnActionEvent): Unit =
    LOGIN_LOGOUT_KEY.getData(e.getDataContext) match {
      case null =>
        e.getPresentation.setEnabledAndVisible(false)
      case provider =>
        e.getPresentation.setVisible(true)
        if provider.hasLoggedIn then e.getPresentation.setEnabledAndVisible(false)
        else if provider.isLoggingIn then
          e.getPresentation.setEnabled(false)
          e.getPresentation.putClientProperty(LOADING_KEY, true)
        else
          e.getPresentation.setEnabled(true)
          e.getPresentation.putClientProperty(LOADING_KEY, false)
    }

  override def createCustomComponent(presentation: Presentation, place: String): JComponent = {
    LoginActionComponent(presentation, place)
  }

  override def updateCustomComponent(component: JComponent, presentation: Presentation): Unit = {
    if presentation.getClientProperty(LOADING_KEY) then
      component.asInstanceOf[LoginActionComponent].select(ComponentType.LOADING, false)
    else component.asInstanceOf[LoginActionComponent].select(ComponentType.BUTTON, false)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  private enum ComponentType {
    case BUTTON
    case LOADING
  }

  private val LOADING_KEY = Key.create[Boolean]("LOGIN_STATUS_KEY")
  private class LoginActionComponent(private val presentation: Presentation, private val place: String)
      extends CardLayoutPanel[ComponentType, ComponentType, JComponent] {
    override def prepare(key: ComponentType): ComponentType = key

    override def create(ui: ComponentType): JComponent = ui match
      case ComponentType.BUTTON =>
        ActionButton(LoginAction.this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
      case ComponentType.LOADING =>
        val label = JBLabel()
        label.setIcon(CodeEpiphanyIcons.LOADING)
        label.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
        label
  }
}

object LoginAction {
  final val LOGIN_LOGOUT_KEY = DataKey.create[LoginLogoutProvider]("LOGIN_LOGOUT_KEY")
  trait LoginLogoutProvider {
    def login(): Unit
    def logout(): Unit
    def isLoggingIn: Boolean
    def hasLoggedIn: Boolean
  }

}
