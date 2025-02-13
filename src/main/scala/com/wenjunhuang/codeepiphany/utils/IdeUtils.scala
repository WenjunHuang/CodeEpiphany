package com.wenjunhuang.codeepiphany.utils

import com.intellij.openapi.application.ApplicationInfo

object IdeUtils {
  val majorVersion: Int = ApplicationInfo.getInstance().getMajorVersion.toInt
}
