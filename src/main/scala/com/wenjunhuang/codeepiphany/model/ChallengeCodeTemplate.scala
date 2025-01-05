package com.wenjunhuang.codeepiphany.model

import com.wenjunhuang.codeepiphany.hackerrank.model.ChallengeDifficulty

import scala.beans.BeanProperty

case class ChallengeCodeTemplate(
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
  language: Language
) {
  def getHeader: String = language.makeCodeRegion(header)

  def getTemplate: String = language.makeCodeRegion(template)

  def getTail: String = language.makeCodeRegion(tail)

  def getCode: String = language.makeCodeRegion("$header\n$template\n$tail")
  
  def getDojo:String = dojo.value
  
}
