package com.wenjunhuang.codeepiphany.model

import scala.annotation.static

trait Constants{}
object Constants {
  @static
  final val ProjectName ="CodeEpiphany"
  @static
  final val ProjectId = "com.wenjun.codeEpiphany"

  @static
  final val QuestionStorageFile = ProjectName + "/questions.xml"

  val HackerRankLoginTopic: String = ProjectId + ".hackerrank.login.topic"
}
