package com.wenjunhuang.codeepiphany.utils

import cats.effect.kernel.Async
import cats.effect.unsafe.{ IORuntime, IORuntimeConfig, Scheduler }
import java.util.concurrent.Executors
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }

import com.intellij.openapi.application.{ ApplicationManager, ModalityState }
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.progress.{ ProgressIndicator, ProgressManager, Task }
import com.intellij.openapi.project.Project

private trait IOOps {
  val intellijComputeContext: ExecutionContextExecutorService = ExecutionContextExecutorServiceBridge(
    ExecutionContext.fromExecutor(ApplicationManagerEx.getApplicationEx.executeOnPooledThread(_))
  )
  val intellijEDTAnyContext: ExecutionContext =
    ExecutionContext.fromExecutor(ApplicationManager.getApplication.invokeLater(_, ModalityState.any()))

  val intellijEDTDefaultContext: ExecutionContext =
    ExecutionContext.fromExecutor(ApplicationManager.getApplication.invokeLater(_))

  val intellijWriteContext: ExecutionContext =
    ExecutionContext.fromExecutor(runnable =>
      ApplicationManager.getApplication.invokeLaterOnWriteThread(() => {
        ApplicationManager.getApplication.runWriteAction(new Runnable {
          override def run(): Unit = {
            runnable.run()
          }
        })
      })
    )

  def intellijProgressContext(project: Project, taskName: String): ExecutionContext =
    ExecutionContext.fromExecutor(runnable =>
      ProgressManager
        .getInstance()
        .run(new Task.Backgroundable(project, taskName, true) {
          override def run(indicator: ProgressIndicator): Unit =
            runnable.run()
        })
    )
  val intellijIOScheduler: Scheduler = Scheduler.fromScheduledExecutor(Executors.newSingleThreadScheduledExecutor())

  implicit val intellijIORuntime: IORuntime = IORuntime(
    compute = intellijComputeContext,
    blocking = intellijComputeContext,
    scheduler = intellijIOScheduler,
    shutdown = () => (),
    config = IORuntimeConfig()
  )

  extension [F[_]: Async, A](fa: F[A]) {
    def evalOnEDTAny(): F[A]     = Async[F].evalOn(fa, intellijEDTAnyContext)
    def evalOnEDTDefault(): F[A] = Async[F].evalOn(fa, intellijEDTDefaultContext)
    def evalOnWrite(): F[A]      = Async[F].evalOn(fa, intellijWriteContext)
  }
}
