package com.wenjunhuang.codeepiphany.utils

import cats.effect.IO
import scala.concurrent.duration.*

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.{ProgressIndicator, Task}
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.utils.syntax.*

trait IOExtensionsOps {
  private val myLogger = Logger.getInstance(getClass.getName)

  extension [A](io: IO[A]) {
    def unsafeRunAsBackgroundProgressCancellable(project: Project, taskName: String): Unit =
      evalAsBackgroundProgressCancellable(project, taskName).unsafeRunSync()

    def evalAsBackgroundProgressCancellable(project: Project, taskName: String): IO[Unit] = {
      IO.delay {
        new Task.Backgroundable(project, taskName) {
          override def run(indicator: ProgressIndicator): Unit = {
            runCancellable(io, indicator)
          }
        }.queue()
      }.void
    }
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
