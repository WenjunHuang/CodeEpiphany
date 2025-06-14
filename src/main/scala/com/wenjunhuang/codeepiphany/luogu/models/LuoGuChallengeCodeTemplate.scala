package com.wenjunhuang.codeepiphany.luogu.models

import scala.beans.BeanProperty

import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService

case class LuoGuChallengeCodeTemplate(
  @BeanProperty
  id: String,
  @BeanProperty
  title: String,
  language: Language,
  languageVersion: LanguageVersion,
  description: String
) {
  def getCodeDojo: String = CodeDojo.LuoGu.value
}

object LuoGuChallengeCodeTemplate {
  implicit val testCasesHolder
    : BaseOpenChallengeService.TestCasesHolder[LuoGuChallengeCodeTemplate] =
    new BaseOpenChallengeService.TestCasesHolder[LuoGuChallengeCodeTemplate] {
      override def getTestCases(
        template: LuoGuChallengeCodeTemplate
      ): List[com.wenjunhuang.codeepiphany.settings.ChallengeSettings.TestCase] = Nil
    }
}
