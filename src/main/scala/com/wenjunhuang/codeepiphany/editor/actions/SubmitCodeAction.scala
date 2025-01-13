package com.wenjunhuang.codeepiphany.editor.actions

import cats.effect.IO
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.editor.actions.SubmitCodeAction.*
import com.wenjunhuang.codeepiphany.editor.services.{runCode, submitCode}
import com.wenjunhuang.codeepiphany.services.http.{HttpClientKeeper, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

class SubmitCodeAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    getProvider(e) match
      case Some(provider) =>
        provider.submitCurrent()
      case None =>
  }

  override def update(e: AnActionEvent): Unit = {
    getProvider(e) match
      case Some(provider) =>
        e.getPresentation.setEnabledAndVisible(true)
      case None =>
        e.getPresentation.setEnabledAndVisible(false)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  private def getProvider(e: AnActionEvent): Option[SubmitCodeProvider] = {
    Option(e.getData(PlatformCoreDataKeys.FILE_EDITOR)).flatMap { editor =>
      Option(SUBMITCODE_PROVIDER_KEY.get(editor))
    }
  }
}
object SubmitCodeAction {
  val SUBMITCODE_PROVIDER_KEY: Key[SubmitCodeProvider] = Key[SubmitCodeProvider]("SubmitCodeProvider")

  trait SubmitCodeProvider {
    def submitCurrent(): Unit

    def runCurrent(): Unit
  }

  object SubmitCodeProvider {

    def createProvider(vf: VirtualFile, project: Project): SubmitCodeProvider = new SubmitCodeProvider:
      implicit val httpClientKeeper: HttpClientKeeper[IO] = HttpClientService.getInstance(project).httpClientKeeper

      override def submitCurrent(): Unit = {
        submitCode[IO](vf, project)
          .unsafeRunAsBackgroundProgressCancellable(project, "Submitting code")
      }

      override def runCurrent(): Unit = {
        runCode[IO](vf, project)
          .unsafeRunAsBackgroundProgressCancellable(project, "Running code")
      }
  }
}
