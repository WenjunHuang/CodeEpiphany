package com.wenjunhuang.codeepiphany

import com.intellij.DynamicBundle
import org.jetbrains.annotations.{NotNull, PropertyKey}

import scala.annotation.varargs

object PluginBundle {
  @varargs
  def message(@PropertyKey(resourceBundle = BUNDLE) key: String, @NotNull params: Any*): String =
    INSTANCE.getMessage(key, params*)

  def message(@PropertyKey(resourceBundle = BUNDLE) key: String): String = INSTANCE.getMessage(key)

  def messageOfBuildKey(key: String): String = INSTANCE.messageOrNull(key)
  
  final private val BUNDLE = "messages.PluginBundle"
  private val INSTANCE     = DynamicBundle(getClass, BUNDLE)
}
