package com.wenjunhuang.codeepiphany
import cats.effect.kernel.Async
import cats.effect.unsafe.{ IORuntime, IORuntimeConfig, Scheduler }
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.diagnostic.Logger

import java.util
import java.util.concurrent.{ Executor, Executors }
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }

package object utils {
  val intellijComputeContext: ExecutionContextExecutorService = ExecutionContextExecutorServiceBridge(ExecutionContext.fromExecutor(ApplicationManagerEx.getApplicationEx.executeOnPooledThread(_)))
  val intellijUIContext: ExecutionContext                     = ExecutionContext.fromExecutor(ApplicationManager.getApplication.invokeLater(_))
  val scheduler: Scheduler                                    = Scheduler.fromScheduledExecutor(Executors.newSingleThreadScheduledExecutor())
  final val Log: Logger                                       = Logger.getInstance("com.wenjunhuang.codeepiphany")

  given intellijIORuntime: IORuntime = IORuntime(
    compute = intellijComputeContext,
    blocking = intellijComputeContext,
    scheduler = scheduler,
    shutdown = () => (),
    config = IORuntimeConfig()
  )

  def evalOnUI[F[_]: Async, A](fa: F[A]): F[A] = Async[F].evalOn(fa, intellijUIContext)
}
