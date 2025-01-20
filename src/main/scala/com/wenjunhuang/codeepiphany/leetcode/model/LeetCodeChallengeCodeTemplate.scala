package com.wenjunhuang.codeepiphany.leetcode.model

import scala.beans.BeanProperty

import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }

case class LeetCodeChallengeCodeTemplate(
  @BeanProperty
  questionId: String,
  @BeanProperty
  frontendQuestionId:String,
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
  languageVersion: LanguageVersion
) {
  def getCode: String            = language.makeCodeRegion(s"$code")
  def getCodeDojo: String        = dojo.value
  def getLanguage: String        = language.value
  def getLanguageVersion: String = languageVersion.version

  override def toString: String = ""
}
