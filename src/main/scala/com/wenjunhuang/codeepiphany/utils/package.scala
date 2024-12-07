package com.wenjunhuang.codeepiphany
import com.intellij.openapi.util.registry.RegistryManager

package object utils {
  object implicits  extends IOOps with LoggerOps with CefOps {}
  object extensions extends CefExtensionsOps     {}

  final val isDebug: Boolean = RegistryManager.getInstance().is("codeepiphany.debug")
}
