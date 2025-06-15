package com.wenjunhuang.codeepiphany.luogu.models

import scala.beans.BeanProperty

import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.PluginBundle

case class LuoGuChallengeCodeTemplate(
  @BeanProperty
  id: String,
  @BeanProperty
  title: String,
  language: Language,
  languageVersion: LanguageVersion,
  description: String,
  testCases: List[ChallengeSettings.TestCase]
) {
  def getCodeDojo: String = CodeDojo.LuoGu.value
  def getTestCases: String = {
    if testCases.isEmpty then ""
    else
      testCases.zipWithIndex.map { (testCase, index) =>
        s"""
           |${PluginBundle.message("testcases.title", index + 1)}:
           |-----------------------------------------------------
           |input:
           |${testCase.input}
           |-----------------------------------------------------
           |expected output:
           |${testCase.expectedOutput}""".stripMargin
      }.mkString("\n")
  }
}

object LuoGuChallengeCodeTemplate {
  implicit val testCasesHolder: BaseOpenChallengeService.TestCasesHolder[LuoGuChallengeCodeTemplate] =
    (template: LuoGuChallengeCodeTemplate) => template.testCases
}
