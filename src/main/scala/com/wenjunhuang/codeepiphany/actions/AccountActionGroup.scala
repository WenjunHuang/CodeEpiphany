package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{AnActionEvent, DataKey, DefaultActionGroup}

import com.wenjunhuang.codeepiphany.actions.LoginAction.LOGIN_LOGOUT_KEY
import com.wenjunhuang.codeepiphany.actions.UserAccountInfoAction.USER_ACCOUNT_INFO_KEY
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, MultipleDataKeysNotNull, UserLoggedIn}

class AccountActionGroup extends DefaultActionGroup with ActionCompatible with MultipleDataKeysNotNull  {
  override protected def dataKeys: Seq[DataKey[?]] = Seq(LOGIN_LOGOUT_KEY, USER_ACCOUNT_INFO_KEY)

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    if (isSatisfied(e)) {
      if (getValue(e, LOGIN_LOGOUT_KEY).hasLoggedIn) {
        presentation.setEnabledAndVisible(true)
        val provider = getValue(e, USER_ACCOUNT_INFO_KEY)
        presentation.setIcon(provider.avatar)
      } else {
        presentation.setEnabledAndVisible(false)
      }
    }
    else {
      presentation.setEnabledAndVisible(false)
    }
  }

}
