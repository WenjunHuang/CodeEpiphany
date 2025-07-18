package com.wenjunhuang.codeepiphany.luogu.models

import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

import scala.beans.BeanProperty

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
  def getTestCases: String = testCases.show
}

object LuoGuChallengeCodeTemplate {
  implicit val testCasesHolder: BaseOpenChallengeService.TestCasesHolder[LuoGuChallengeCodeTemplate] =
    (template: LuoGuChallengeCodeTemplate) => template.testCases
}
