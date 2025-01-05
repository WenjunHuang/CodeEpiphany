package com.wenjunhuang.codeepiphany.editor.actions.providers

import com.intellij.openapi.actionSystem.DataKey

trait SubmitCodeProvider {
  def submitCurrent(): Unit
  def runCurrent(): Unit
}

object SubmitCodeProvider {
  val SUBMITCODE_PROVIDER_KEY: DataKey[SubmitCodeProvider] = DataKey.create[SubmitCodeProvider]("SubmitCodeProvider")
}
