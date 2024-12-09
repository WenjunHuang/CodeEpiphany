package com.wenjunhuang.codeepiphany.model

import com.intellij.util.messages.Topic

object messages {
  trait LoginLogoutNotifier {
    def login(codeDojo: CodeDojo): Unit
    def logout(codeDojo:CodeDojo):Unit
  }
  
  @Topic.ProjectLevel
  val LOGIN_LOGOUT_TOPIC: Topic[LoginLogoutNotifier] = Topic(classOf[LoginLogoutNotifier])

}
