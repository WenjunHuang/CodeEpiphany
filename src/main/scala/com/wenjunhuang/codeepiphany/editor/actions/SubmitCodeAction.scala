package com.wenjunhuang.codeepiphany.editor.actions

import cats.effect.IO

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.editor.actions.SubmitCodeAction.*
import com.wenjunhuang.codeepiphany.editor.services.{ runCode, submitCode }
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import com.wenjunhuang.codeepiphany.services.AuthService
import com.wenjunhuang.codeepiphany.services.file.saveEditedFile
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
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
    val enabled = getProvider(e).exists(_.canSubmit)
    e.getPresentation.setEnabled(enabled)
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

    def canSubmit: Boolean

    def runCurrent(): Unit

    def enabledRun: Boolean
    def canRun: Boolean
  }

  object SubmitCodeProvider {

    def createProvider(vf: VirtualFile, project: Project, codeDojo: CodeDojo): SubmitCodeProvider =
      new SubmitCodeProvider:
        implicit val httpClientKeeper: HttpClientManager[IO] = HttpClientService.getInstance(project).httpClientManager

        override def canSubmit: Boolean = {
          ChallengeSettings.getInstance(project).findChallengeId(vf).exists { challenge =>
            AuthService.getInstance(project).isLoggedIn(challenge.dojo)
          }
        }

        override def canRun: Boolean = {
          ChallengeSettings.getInstance(project).findChallengeId(vf).exists { challenge =>
            AuthService.getInstance(project).isLoggedIn(challenge.dojo)
          }
        }

        override def submitCurrent(): Unit = {
          if canSubmit then
            (saveEditedFile[IO](vf) *>
              submitCode[IO](vf, project))
              .unsafeRunAsBackgroundProgressCancellable(project, "Submitting code")
        }

        override def runCurrent(): Unit = {
          if canRun then
            (saveEditedFile[IO](vf) *>
              runCode[IO](vf, project))
              .unsafeRunAsBackgroundProgressCancellable(project, "Running code")
        }

        override def enabledRun: Boolean =
          codeDojo match
            case CodeForces => false
            case _          => true

  }
}
