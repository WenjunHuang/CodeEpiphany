package com.wenjunhuang.codeepiphany.editor.actions.providers

import cats.effect.IO
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.editor.services.runCode
import com.wenjunhuang.codeepiphany.services.http.{HttpClientKeeper, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.extensions.*

trait SubmitCodeProvider {
  def submitCurrent(): Unit
  def runCurrent(): Unit
}

object SubmitCodeProvider {
  val SUBMITCODE_PROVIDER_KEY: Key[SubmitCodeProvider] = Key[SubmitCodeProvider]("SubmitCodeProvider")

  def createProvider(vf: VirtualFile, project: Project): SubmitCodeProvider = new SubmitCodeProvider:
    implicit val httpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(project).httpClientKeeper

    override def submitCurrent(): Unit = ???

    override def runCurrent(): Unit = {
      runCode[IO](vf, project)
        .unsafeRunAsBackgroundProgressCancellable(project, "Running code")
    }
}
