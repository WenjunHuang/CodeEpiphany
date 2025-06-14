package com.wenjunhuang.codeepiphany.test

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}

import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, ProjectNonNull}
import com.wenjunhuang.codeepiphany.utils.testCases.TestCasesDialog

class TestAction extends AnAction with ProjectNonNull with ActionCompatible{


  override def actionPerformed(e: AnActionEvent): Unit =  {
    TestCasesDialog(e.getProject, List(
      ("1 2 3", "6"),
      ("4 5 6", "15"),
      ("7 8 9", "24")
    )).show()
  }
}
