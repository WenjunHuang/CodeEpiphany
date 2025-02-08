package com.wenjunhuang.codeepiphany

import org.jetbrains.annotations.{NotNull, PropertyKey}
import scala.annotation.varargs

import com.intellij.DynamicBundle

object PluginBundle {
  @varargs
  def message(@PropertyKey(resourceBundle = BUNDLE) key: String, @NotNull params: Any*): String =
    INSTANCE.getMessage(key, params*)

  def message(@PropertyKey(resourceBundle = BUNDLE) key: String): String = INSTANCE.getMessage(key)

  def messageOfBuildKey(key: String): String = INSTANCE.getMessage(key)
  
  final private val BUNDLE = "messages.PluginBundle"
  private val INSTANCE     = DynamicBundle(getClass, BUNDLE)
}
