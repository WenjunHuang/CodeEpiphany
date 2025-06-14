package com.wenjunhuang.codeepiphany.editor.actions

import com.intellij.openapi.actionSystem.{ AnAction, AnActionEvent }
import com.intellij.openapi.util.Key

import com.wenjunhuang.codeepiphany.editor.actions.TestCasesEditionAction.{
  TESTCASES_PROVIDER_KEY,
  TestCasesEditionProvider
}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.actions.{
  ActionCompatible,
  FileEditorKeyNotNull,
  FileEditorUserLoggedIn,
  ProjectNonNull
}
import com.wenjunhuang.codeepiphany.utils.testCases.TestCasesDialog

class TestCasesEditionAction
    extends AnAction
    with ActionCompatible
    with ProjectNonNull
    with FileEditorUserLoggedIn
    with FileEditorKeyNotNull[TestCasesEditionProvider](TESTCASES_PROVIDER_KEY) {

  override def actionPerformed(e: AnActionEvent): Unit = {
    val testCases        = getValue(e).getTestCases
    val defaultTestCases = getValue(e).getDefaultTestCases
    val dialog =
      TestCasesDialog(myProject = e.getProject, myTestCases = testCases, myDefaultTestCases = defaultTestCases)
    if (dialog.showAndGet()) {
      val updatedTestCases = dialog.getTestCases()
      getValue(e).updateTestCases(updatedTestCases)
    }
  }

  override def update(event: AnActionEvent): Unit = {
    if isSatisfied(event) then event.getPresentation.setEnabled(true)
    else event.getPresentation.setEnabled(false)
  }
}

object TestCasesEditionAction {
  val TESTCASES_PROVIDER_KEY: Key[TestCasesEditionProvider] = Key[TestCasesEditionProvider]("TestCasesEditionProvider")
  trait TestCasesEditionProvider {
    def getDefaultTestCases: List[ChallengeSettings.TestCase]
    def getTestCases: List[ChallengeSettings.TestCase]
    def updateTestCases(testCases: List[ChallengeSettings.TestCase]): Unit
  }
}
