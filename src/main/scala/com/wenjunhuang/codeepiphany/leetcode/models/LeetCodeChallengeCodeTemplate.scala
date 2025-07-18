package com.wenjunhuang.codeepiphany.leetcode.models

import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService.TestCasesHolder
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.TestCase

import scala.beans.BeanProperty

case class LeetCodeChallengeCodeTemplate(
  @BeanProperty
  questionId: String,
  @BeanProperty
  frontendQuestionId: String,
  dojo: CodeDojo,
  @BeanProperty
  name: String,
  code: String,
  @BeanProperty
  slug: String,
  @BeanProperty
  description: String,
  @BeanProperty
  difficulty: String,
  language: Language,
  languageVersion: LanguageVersion,
  content: LeetCodeChallengeData,
  testCases: List[TestCase]
) {
  def getCode: String            = language.encloseCodeInRegion(s"$code")
  def getCodeDojo: String        = dojo.value
  def getLanguage: String        = language.value
  def getLanguageVersion: String = languageVersion.version
  def getTestCases: String       = testCases.show

  override def toString: String = ""
}
object LeetCodeChallengeCodeTemplate {
  implicit val testCasesHolder: TestCasesHolder[LeetCodeChallengeCodeTemplate] =
    new TestCasesHolder[LeetCodeChallengeCodeTemplate] {
      override def getTestCases(template: LeetCodeChallengeCodeTemplate): List[TestCase] = template.testCases
    }
}
