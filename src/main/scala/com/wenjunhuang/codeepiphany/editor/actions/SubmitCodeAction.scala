package com.wenjunhuang.codeepiphany.editor.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.editor.actions.SubmitCodeAction.*
import com.wenjunhuang.codeepiphany.editor.services.submitCode
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.AuthService
import com.wenjunhuang.codeepiphany.services.file.saveEditedFile
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.actions.{ ActionCompatible, FileEditorKeyNotNull, FileEditorUserLoggedIn }
import com.wenjunhuang.codeepiphany.utils.extensions.*

class SubmitCodeAction
    extends AnAction
    with ActionCompatible
    with FileEditorUserLoggedIn
    with FileEditorKeyNotNull[SubmitCodeProvider](SUBMITCODE_PROVIDER_KEY) {
  override def actionPerformed(e: AnActionEvent): Unit = getValue(e).submitCurrent()

  override def update(event: AnActionEvent): Unit = {
    if isSatisfied(event) then event.getPresentation.setEnabledAndVisible(true)
    else event.getPresentation.setEnabledAndVisible(false)
  }
}

object SubmitCodeAction {
  val SUBMITCODE_PROVIDER_KEY: Key[SubmitCodeProvider] = Key[SubmitCodeProvider]("SubmitCodeProvider")

  trait SubmitCodeProvider {
    def submitCurrent(): Unit
  }

  object SubmitCodeProvider {

    def createProvider(vf: VirtualFile, project: Project, codeDojo: CodeDojo): SubmitCodeProvider =
      new SubmitCodeProvider {
        override def submitCurrent(): Unit = {
          (saveEditedFile(vf) *>
            submitCode(vf, project))
            .unsafeRunAsBackgroundProgressCancellable(project, "Submitting code")
        }
      }

  }
}
