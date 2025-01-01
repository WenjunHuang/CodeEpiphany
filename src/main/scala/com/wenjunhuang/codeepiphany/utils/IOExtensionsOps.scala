package com.wenjunhuang.codeepiphany.utils

import cats.effect.IO
import com.intellij.openapi.progress.{ PerformInBackgroundOption, ProgressIndicator, ProgressManager, Task }
import com.intellij.openapi.project.Project

import scala.concurrent.duration.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

trait IOExtensionsOps {
  extension [A](io: IO[A]) {
    def unsafeRunAsConditionalModal(project: Project, taskName: String): Unit = ProgressManager
      .getInstance()
      .run(new Task.ConditionalModal(project, taskName, false, PerformInBackgroundOption.DEAF) {
        override def run(indicator: ProgressIndicator): Unit = io.unsafeRunSync()
      })

    def unsafeRunAsConditionalModalCancellable(project: Project, taskName: String): Unit = ProgressManager
      .getInstance()
      .run(new Task.ConditionalModal(project, taskName, true, PerformInBackgroundOption.DEAF) {
        override def run(indicator: ProgressIndicator): Unit = runCancellable(io, indicator)
      })

    def unsafeRunAsModal(project: Project, taskName: String): Unit = ProgressManager
      .getInstance()
      .run(new Task.Modal(project, taskName, false) {
        override def run(indicator: ProgressIndicator): Unit = io.unsafeRunSync()
      })

    def unsafeRunAsModalCancellable(project: Project, taskName: String): Unit = ProgressManager
      .getInstance()
      .run(new Task.Modal(project, taskName, true) {
        override def run(indicator: ProgressIndicator): Unit = runCancellable(io, indicator)
      })

    def unsafeRunAsBackgroundProgress(project: Project, taskName: String): Unit = ProgressManager
      .getInstance()
      .run(new Task.Backgroundable(project, taskName, false) {
        override def run(indicator: ProgressIndicator): Unit = {
          io.unsafeRunSync()
        }
      })

    def unsafeRunAsBackgroundProgressCancellable(project: Project, taskName: String): Unit = ProgressManager
      .getInstance()
      .run(new Task.Backgroundable(project, taskName) {
        override def run(indicator: ProgressIndicator): Unit = {
          runCancellable(io, indicator)
        }
      })
  }

  private def runCancellable[A](io: IO[A], indicator: ProgressIndicator): Unit = {
    def waitForCancel(): IO[Unit] =
      IO.sleep(100.millis).iterateUntil(_ => indicator.isCanceled)

    io.racePair(waitForCancel())
      .flatMap {
        case Left((_, fiber2))  => fiber2.cancel
        case Right((fiber1, _)) => fiber1.cancel
      }
      .unsafeRunSync()
  }
}
