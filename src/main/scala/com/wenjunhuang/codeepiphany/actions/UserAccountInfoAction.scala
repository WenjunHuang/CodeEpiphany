package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{ AnActionEvent, DataKey }
import com.intellij.openapi.project.DumbAwareAction

import com.wenjunhuang.codeepiphany.actions.UserAccountInfoAction.{ USER_ACCOUNT_INFO_KEY, UserInfoProvider }
import com.wenjunhuang.codeepiphany.utils.actions.{ ActionCompatible, DataKeyNotNull }
import com.wenjunhuang.codeepiphany.utils.AsyncAvatarLoader

class UserAccountInfoAction
    extends DumbAwareAction
    with DataKeyNotNull[UserInfoProvider](USER_ACCOUNT_INFO_KEY)
    with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit = {
    getValue(e).action()
  }

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    if isSatisfied(e) then
      val provider = getValue(e)
      presentation.setEnabledAndVisible(true)
      presentation.setText(provider.username)
      presentation.setIcon(provider.avatar)
    else {
      presentation.setEnabledAndVisible(false)
    }
  }
}

object UserAccountInfoAction {
  final val USER_ACCOUNT_INFO_KEY = DataKey.create[UserInfoProvider]("USER_ACCOUNT_INFO_KEY")

  trait UserInfoProvider {
    def avatar: AsyncAvatarLoader
    def username: String
    def action: () => Unit
  }
}
