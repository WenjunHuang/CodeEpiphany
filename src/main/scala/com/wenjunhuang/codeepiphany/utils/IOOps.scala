package com.wenjunhuang.codeepiphany.utils

import cats.effect.kernel.Async
import cats.effect.unsafe.{ IORuntime, IORuntimeConfig, Scheduler }
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx

import java.util.concurrent.Executors
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }

private trait IOOps {
  val intellijComputeContext: ExecutionContextExecutorService = ExecutionContextExecutorServiceBridge(ExecutionContext.fromExecutor(ApplicationManagerEx.getApplicationEx.executeOnPooledThread(_)))
  val intellijUIContext: ExecutionContext                     = ExecutionContext.fromExecutor(ApplicationManager.getApplication.invokeLater(_))
  val intellijIOScheduler: Scheduler                          = Scheduler.fromScheduledExecutor(Executors.newSingleThreadScheduledExecutor())

  implicit val intellijIORuntime: IORuntime = IORuntime(
    compute = intellijComputeContext,
    blocking = intellijComputeContext,
    scheduler = intellijIOScheduler,
    shutdown = () => (),
    config = IORuntimeConfig()
  )

  extension [F[_]: Async, A](fa: F[A]) {
    def evalOnUI: F[A] = Async[F].evalOn(fa, intellijUIContext)
  }
}
