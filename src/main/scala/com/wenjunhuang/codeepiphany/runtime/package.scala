package com.wenjunhuang.codeepiphany
import cats.effect.kernel.Async
import cats.effect.unsafe.{IORuntime, IORuntimeConfig, Scheduler}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx

import java.util
import java.util.concurrent.{Executor, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

package object runtime {
  val intellijComputeContext: ExecutionContextExecutorService = ExecutionContextExecutorServiceBridge(ExecutionContext.fromExecutor(ApplicationManagerEx.getApplicationEx.executeOnPooledThread(_)))
  val intellijUIContext: ExecutionContext                     = ExecutionContext.fromExecutor(ApplicationManager.getApplication.invokeLater(_))
  val scheduler: Scheduler                                    = Scheduler.fromScheduledExecutor(Executors.newSingleThreadScheduledExecutor())

  given intellijIORuntime: IORuntime = IORuntime(
    compute = intellijComputeContext,
    blocking = intellijComputeContext,
    scheduler = scheduler,
    shutdown = () => (),
    config = IORuntimeConfig()
  )

  def evalOnUI[F[_]: Async, A](fa: F[A]): F[A] = Async[F].evalOn(fa, intellijUIContext)
}
