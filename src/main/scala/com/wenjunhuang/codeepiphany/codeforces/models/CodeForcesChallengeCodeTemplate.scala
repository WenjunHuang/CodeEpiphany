package com.wenjunhuang.codeepiphany.codeforces.models

import cats.syntax.all.*
import scala.beans.BeanProperty

import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.BaseOpenChallengeService
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.PluginBundle

case class CodeForcesChallengeCodeTemplate(
  @BeanProperty
  contestIdIndex: String,
  @BeanProperty
  name: String,
  language: Language,
  languageVersion: LanguageVersion,
  rating: Option[Int],
  problemsetName: Option[String],
  tags: List[String],
  content: CodeForcesChallengeData,
  testCases: List[ChallengeSettings.TestCase]
) {
  def getCodeDojo: String        = CodeDojo.CodeForces.show
  def getLanguage: String        = language.show
  def getLanguageVersion: String = languageVersion.version
  def getRating: String          = rating.map(_.toString).getOrElse("")
  def getProblemsetName: String  = problemsetName.getOrElse("")
  def getTags: String            = tags.mkString(", ")
  def getTestCases: String = testCases.show


}

object CodeForcesChallengeCodeTemplate {
  implicit val testCasesHolder: BaseOpenChallengeService.TestCasesHolder[CodeForcesChallengeCodeTemplate] =
    (template: CodeForcesChallengeCodeTemplate) => template.testCases
}
