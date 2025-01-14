package com.wenjunhuang.codeepiphany.utils

import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.Resource.ExitCase.{Canceled, Errored, Succeeded}
import cats.effect.{IO, OutcomeIO}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.{PerformInBackgroundOption, ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.utils.implicits.*

import scala.concurrent.duration.*

trait IOExtensionsOps {
  private val myLogger = Logger.getInstance(getClass.getName)
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

    def evalAsBackgroundProgress(project: Project, taskName: String): IO[A] =
      IO.deferred[ExitCase].flatMap { deferred =>
        IO.delay {
          ProgressManager
            .getInstance()
            .run(new Task.Backgroundable(project, taskName) {
              override def run(indicator: ProgressIndicator): Unit = {
                deferred.get.unsafeRunSync() match
                  case ExitCase.Canceled   => indicator.cancel()
                  case ExitCase.Errored(e) => indicator.cancel()
                  case ExitCase.Succeeded  => ()
              }
            })
        } *> io
          .onCancel(deferred.complete(Canceled).void)
          .onError(e => deferred.complete(Errored(e)).void)
          .flatMap(result => deferred.complete(Succeeded).map(_ => result))
      }

//    def evalAsBackgroundProgressCancellable(project: Project, taskName: String): IO[A] =
//      IO.deferred[Unit].flatMap { signal =>
//        IO.deferred[Unit].flatMap { deferred =>
//          IO.delay{
//            ProgressManager
//              .getInstance()
//              .run(new Task.Backgroundable(project, taskName, true) {
//                override def run(indicator: ProgressIndicator): Unit = {
//                  def waitForCancel(): IO[Unit] =
//                    IO.sleep(100.millis).iterateUntil(_ => indicator.isCanceled)
//
//                  val toRun = deferred.get.racePair(waitForCancel()).flatMap {
//                    case Left((l, fiber2)) => fiber2.cancel
//                    case Right((fiber1, r)) => fiber1.cancel
//                  }
//
//                  try toRun.unsafeRunSync()
//                  catch case _ => indicator.cancel()
//                }
//              })
//          } *>
//            io.start.flatMap(fiber => fiber.join)
//        }
//      }
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

//  private def runCancellableIO[A](io: IO[A], indicator: ProgressIndicator): IO[Either[Unit, OutcomeIO[A]]] = {
//    def waitForCancel(): IO[Unit] =
//      IO.sleep(100.millis).iterateUntil(_ => indicator.isCanceled)
//
//    io.racePair(waitForCancel()).flatMap {
//      case Left((l, fiber2))  => fiber2.cancel *> l.asRight.pure[IO]
//      case Right((fiber1, r)) => fiber1.cancel *> ().asLeft.pure[IO]
//    }
}
