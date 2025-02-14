package com.wenjunhuang.codeepiphany.utils

import com.intellij.openapi.application.ApplicationInfo

object IdeUtils {
  def majorVersion: Int    = ApplicationInfo.getInstance().getMajorVersion.toInt
  def minorVersion: Int    = ApplicationInfo.getInstance().getMinorVersion.toInt
  def shortVersion: String = ApplicationInfo.getInstance().getShortVersion
}
