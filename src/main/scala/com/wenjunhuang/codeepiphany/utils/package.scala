package com.wenjunhuang.codeepiphany
import com.intellij.openapi.util.registry.RegistryManager

package object utils {
  object syntax     extends IOOps with LoggerOps with KotlinCompOps with SwingOps         {}
  object extensions extends CefExtensionsOps with IOExtensionsOps with SwingExtensionsOps {}

  final val isDebug: Boolean = RegistryManager.getInstance().is("codeepiphany.debug")
}
