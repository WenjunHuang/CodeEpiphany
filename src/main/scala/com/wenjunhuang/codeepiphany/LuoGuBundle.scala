package com.wenjunhuang.codeepiphany

import org.jetbrains.annotations.{ NotNull, PropertyKey }
import scala.annotation.varargs

import com.intellij.DynamicBundle

import com.wenjunhuang.codeepiphany.PluginBundle.getClass

object LuoGuBundle {
  @varargs
  def message(@PropertyKey(resourceBundle = BUNDLE) key: String, @NotNull params: Any*): String =
    INSTANCE.getMessage(key, params*)

  def message(@PropertyKey(resourceBundle = BUNDLE) key: String): String = INSTANCE.getMessage(key)

  def messageOfBuildKey(key: String): String = INSTANCE.messageOrNull(key)

  private final val BUNDLE = "messages.LuoGuBundle"
  private val INSTANCE     = DynamicBundle(getClass, BUNDLE)
}
