package com.wenjunhuang.codeepiphany.editor.actions.providers

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import cats.effect.IO
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.LogConsoleView
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.extensions.*

trait SubmitCodeProvider {
  def submitCurrent(): Unit
  def runCurrent(): Unit
}

object SubmitCodeProvider {
  val SUBMITCODE_PROVIDER_KEY: Key[SubmitCodeProvider] = Key[SubmitCodeProvider]("SubmitCodeProvider")

  def createProvider(vf: VirtualFile, project: Project): SubmitCodeProvider = new SubmitCodeProvider:
    override def submitCurrent(): Unit = ???
    


    override def runCurrent(): Unit = ???
}
