package com.wenjunhuang.codeepiphany.controllers.dojo

import com.intellij.openapi.actionSystem.DataKey

package object actions {
  type LoginFunction = () => Unit
  
  object keys {
    final val LOGIN_KEY = DataKey.create[LoginFunction]("LOGIN_KEY")
  }
}
