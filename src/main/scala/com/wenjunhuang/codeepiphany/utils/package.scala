package com.wenjunhuang.codeepiphany
import com.intellij.openapi.util.registry.RegistryManager

package object utils {
  object implicits  extends IOOps with LoggerOps with CefOps with KotlinCompOps with SwingOps with ServiceOps {}
  object extensions extends CefExtensionsOps with IOExtensionsOps with SwingExtensionsOps     {}

  final val isDebug: Boolean = RegistryManager.getInstance().is("codeepiphany.debug")
}
