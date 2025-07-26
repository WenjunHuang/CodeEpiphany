package com.wenjunhuang.codeepiphany.utils

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId

object IdeUtils {
  def majorVersion: Int    = ApplicationInfo.getInstance().getMajorVersion.toInt
  def minorVersion: Int    = ApplicationInfo.getInstance().getMinorVersion.toInt
  def shortVersion: String = ApplicationInfo.getInstance().getShortVersion
  def pluginVersion: String =
    Option(PluginManagerCore.getPlugin(PluginId.getId("com.wenjunhuang.codeepiphany"))).map(_.getVersion).getOrElse("")
}
