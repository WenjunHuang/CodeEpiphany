package com.wenjunhuang.codeepiphany.hackerrank.model

import scala.beans.BeanProperty

import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}

case class HackerRankChallengeCodeTemplate(
  @BeanProperty
  dojoId: String,
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
  languageVersion: LanguageVersion
) {
  def getHeader: String = language.makeCodeRegion(header)

  def getTemplate: String = language.makeCodeRegion(template)

  def getTail: String = language.makeCodeRegion(tail)

  def getCode: String = language.makeCodeRegion(s"$header\n$template\n$tail")

  def getCodeDojo: String = dojo.value

  def getLanguage: String        = language.value
  def getLanguageVersion: String = languageVersion.version
}
