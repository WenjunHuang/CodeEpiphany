package com.wenjunhuang.codeepiphany.model

import scala.annotation.static

trait Constants {}
object Constants {
  @static
  final inline val PROJECT_NAME = "CodeEpiphany"

  @static
  final val PROJECT_ID = "com.wenjun.codeepiphany"

  @static
  final val ACTION_PREFIX = PROJECT_NAME + ".actions"

  @static
  final val SETTINGS_FOLDER = PROJECT_NAME

  @static
  final val CHALLENGE_STORAGE_FILE = "challenges.db"

  @static
  final val SETTING = PROJECT_ID + ".settings"

  @static
  final val SETTING_FILE = SETTINGS_FOLDER + "/settings.xml"

  @static
  final val HACKERRANK_SETTING = PROJECT_ID + ".hackerrank.settings"

  @static
  final val HACKERRANK_SETTING_FILE = SETTINGS_FOLDER + "/hackerrank.xml"

}
