package com.wenjunhuang.codeepiphany.model

import scala.annotation.static

trait Constants {}
object Constants {
  @static
  final inline val PROJECT_NAME = "CodeEpiphany"
  
  @static
  final val PROJECT_ID = "com.wenjun.codeEpiphany"

  @static
  final val SETTINGS_FOLDER = PROJECT_NAME + "/settings"

  @static
  final val CHALLENGE_STORAGE_FILE = PROJECT_NAME + "/challenges/challenges.xml"

  @static
  final val HACKERRANK_SETTING = PROJECT_ID + ".hackerrank.settings"

  @static
  final val HACKERRANK_SETTING_FILE = SETTINGS_FOLDER + "/hackerrank.xml"

}
