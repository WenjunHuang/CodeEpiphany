package com.wenjunhuang.codeepiphany

import com.intellij.DynamicBundle
import org.jetbrains.annotations.{ NotNull, PropertyKey }

object PluginBundle {
  def message(@PropertyKey(resourceBundle = BUNDLE) key: String, @NotNull params: Any*): String = INSTANCE.getMessage(key, params*)

  final private val BUNDLE = "messages.PluginBundle"
  private val INSTANCE     = DynamicBundle(getClass, BUNDLE)
}
