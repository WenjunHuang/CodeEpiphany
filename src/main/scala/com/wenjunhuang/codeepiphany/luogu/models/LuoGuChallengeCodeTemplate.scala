package com.wenjunhuang.codeepiphany.luogu.models

import scala.beans.BeanProperty

import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}

case class LuoGuChallengeCodeTemplate(
  @BeanProperty
  id: String,
  @BeanProperty
  name: String,
  language:Language,
  languageVersion:LanguageVersion,
  description:String
) {
  def getCodeDojo: String = CodeDojo.LuoGu.value
}
