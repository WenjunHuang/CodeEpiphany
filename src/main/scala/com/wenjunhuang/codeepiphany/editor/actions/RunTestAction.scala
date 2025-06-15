package com.wenjunhuang.codeepiphany.editor.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.editor.actions.RunTestAction.RunTestProvider
import com.wenjunhuang.codeepiphany.editor.services.runCode
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.file.saveEditedFile
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, FileEditorKeyNotNull, FileEditorUserLoggedIn}
import com.wenjunhuang.codeepiphany.utils.extensions.*

class RunTestAction
    extends AnAction
    with ActionCompatible
    with FileEditorUserLoggedIn
    with FileEditorKeyNotNull[RunTestProvider](RunTestAction.RUNTEST_PROVIDER_KEY) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    getValue(e).runCurrent()
  }

  override def update(event: AnActionEvent): Unit = {
    if isSatisfied(event) then event.getPresentation.setEnabledAndVisible(true)
    else event.getPresentation.setEnabledAndVisible(false)
  }
}

object RunTestAction {
  val RUNTEST_PROVIDER_KEY: Key[RunTestProvider] = Key[RunTestProvider]("RunTestProvider")

  trait RunTestProvider {
    def runCurrent(): Unit
  }

  object RunTestProvider {

    def createProvider(vf: VirtualFile, project: Project, codeDojo: CodeDojo): RunTestProvider =
      new RunTestProvider {
        override def runCurrent(): Unit = {
          (saveEditedFile(vf) *>
            runCode(vf, project))
            .unsafeRunAsBackgroundProgressCancellable(project, "Running Test")
        }
      }
  }
}
