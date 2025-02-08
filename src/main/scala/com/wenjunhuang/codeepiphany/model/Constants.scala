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
  final val CHALLENGE_SETTING = PROJECT_ID + ".challenge.settings"
  @static
  final val CHALLENGE_SETTING_FILE = SETTINGS_FOLDER + "/challenge.xml"

  @static
  final val HACKERRANK_SETTING = PROJECT_ID + ".hackerrank.settings"

  @static
  final val HACKERRANK_SETTING_FILE = SETTINGS_FOLDER + "/hackerrank.xml"

  @static
  final val LEETCODE_CN_SETTING = PROJECT_ID + ".leetcodecn.settings"
  @static
  final val LEETCODE_CN_SETTING_FILE = SETTINGS_FOLDER + "/leetcodecn.xml"
  @static
  final val LEETCODE_SETTING = PROJECT_ID + ".leetcode.settings"
  @static
  final val LEETCODE_SETTING_FILE = SETTINGS_FOLDER + "/leetcode.xml"

  @static
  final val CODEFORCES_SETTING = PROJECT_ID + ".codeforces.settings"
  @static
  final val CODEFORCES_SETTING_FILE = SETTINGS_FOLDER + "/codeforces.xml"

  @static
  final val ATCODER_SETTING = PROJECT_ID + ".atcoder.settings"
  @static
  final val ATCODER_SETTING_FILE = SETTINGS_FOLDER + "/atcoder.xml"

  @static
  final val SUBMIT_CODE_REGION_BEGIN = "IMPORTANT!! Submit Code Region Begin(Do not remove this line)"
  @static
  final val SUBMIT_CODE_REGION_END = "IMPORTANT!! Submit Code Region End(Do not remove this line)"
}
