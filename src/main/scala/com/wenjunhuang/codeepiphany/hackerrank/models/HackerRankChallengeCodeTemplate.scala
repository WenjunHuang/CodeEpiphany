package com.wenjunhuang.codeepiphany.hackerrank.models

import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

import scala.beans.BeanProperty

case class HackerRankChallengeCodeTemplate(
  @BeanProperty
  id: String,
  dojo: CodeDojo,
  @BeanProperty
  name: String,
  @BeanProperty
  slug: String,
  @BeanProperty
  description: String,
  header: String,
  template: String,
  tail: String,
  @BeanProperty
  contest: String,
  @BeanProperty
  difficulty: String,
  language: Language,
  languageVersion: LanguageVersion,
  testCases: List[ChallengeSettings.TestCase]
) {
  def getHeader: String = language.encloseCodeInRegion(header)

  def getTemplate: String = language.encloseCodeInRegion(template)

  def getTail: String = language.encloseCodeInRegion(tail)

  def getCode: String = language.encloseCodeInRegion(s"$header\n$template\n$tail")

  def getCodeDojo: String = dojo.value

  def getLanguage: String        = language.value
  def getLanguageVersion: String = languageVersion.version

  def getTestCases: String = testCases.show
}

object HackerRankChallengeCodeTemplate {
  implicit val testCasesHolder: BaseOpenChallengeService.TestCasesHolder[HackerRankChallengeCodeTemplate] =
    (template: HackerRankChallengeCodeTemplate) => template.testCases
}
