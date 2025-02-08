package com.wenjunhuang.codeepiphany.atcoder.models

import cats.syntax.all.*
import scala.beans.BeanProperty

import com.wenjunhuang.codeepiphany.database.tables.records.AtcoderProblemsRecord
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, LanguageVersion }

case class AtCoderChallengeCodeTemplate(
  @BeanProperty
  contestId: String,
  @BeanProperty
  contestTitle: String,
  @BeanProperty
  id: String,
  @BeanProperty
  problemIndex: String,
  @BeanProperty
  name: String,
  @BeanProperty
  title: String,
  description: String,
  codeDojo: CodeDojo,
  language: Language,
  languageVersion: LanguageVersion,
  record: AtcoderProblemsRecord,
  content: AtCoderChallengeData
) {
  def getLanguage: String        = language.value
  def getLanguageVersion: String = languageVersion.version
  def getCodeDojo: String        = codeDojo.show
}
