package com.wenjunhuang.codeepiphany
import cats.effect.kernel.Async
import cats.effect.unsafe.{IORuntime, IORuntimeConfig, Scheduler}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.RegistryManager

import java.util
import java.util.concurrent.{Executor, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

package object utils extends CefExtensionsOps with IOOps {
  final val Log: Logger = Logger.getInstance("com.wenjunhuang.codeepiphany")
  
  final val isDebug:Boolean = RegistryManager.getInstance().is("codeepiphany.debug")
}
