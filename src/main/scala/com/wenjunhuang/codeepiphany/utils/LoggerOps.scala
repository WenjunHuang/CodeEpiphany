package com.wenjunhuang.codeepiphany.utils

import cats.effect.{IO, Sync, SyncIO}
import cats.syntax.all.*
import com.intellij.openapi.diagnostic.Logger as jLogger
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

trait DiagnosticLoggerFactory[F[_]] extends LoggerFactory[F] {
  def getLoggerFromDiagnosticLogger(logger: jLogger): SelfAwareStructuredLogger[F]

  def fromDiagnosticLogger(logger: jLogger): F[SelfAwareStructuredLogger[F]]
}

object DiagnosticLoggerInternal {
  private def contextLog[F[_]](isEnabled: F[Boolean], ctx: Map[String, String], logging: () => Unit)(implicit
    F: Sync[F]
  ): F[Unit] = {

    val ifEnabled = F.delay(logging())

    isEnabled.ifM(ifEnabled, F.unit)
  }

  final class DiagnosticLogger[F[_]](val logger: jLogger, sync: Sync.Type = Sync.Type.Delay)(implicit F: Sync[F])
      extends SelfAwareStructuredLogger[F] {

    override def isTraceEnabled: F[Boolean] = F.delay(logger.isTraceEnabled)

    override def isDebugEnabled: F[Boolean] = F.delay(logger.isDebugEnabled)

    override def isInfoEnabled: F[Boolean] = F.delay(true)

    override def isWarnEnabled: F[Boolean] = F.delay(true)

    override def isErrorEnabled: F[Boolean] = F.delay(true)

    override def trace(t: Throwable)(msg: => String): F[Unit] =
      isTraceEnabled
        .ifM(
          F.suspend(sync) {
            logger.trace(msg); logger.trace(t)
          },
          F.unit
        )

    override def trace(msg: => String): F[Unit] =
      isTraceEnabled
        .ifM(F.suspend(sync)(logger.trace(msg)), F.unit)

    override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isTraceEnabled, ctx, () => logger.trace(msg))

    override def debug(t: Throwable)(msg: => String): F[Unit] =
      isDebugEnabled
        .ifM(F.suspend(sync)(logger.debug(msg, t)), F.unit)

    override def debug(msg: => String): F[Unit] =
      isDebugEnabled
        .ifM(F.suspend(sync)(logger.debug(msg)), F.unit)

    override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isDebugEnabled, ctx, () => logger.debug(msg))

    override def info(t: Throwable)(msg: => String): F[Unit] =
      isInfoEnabled
        .ifM(F.suspend(sync)(logger.info(msg, t)), F.unit)

    override def info(msg: => String): F[Unit] =
      isInfoEnabled
        .ifM(F.suspend(sync)(logger.info(msg)), F.unit)

    override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isInfoEnabled, ctx, () => logger.info(msg))

    override def warn(t: Throwable)(msg: => String): F[Unit] =
      isWarnEnabled
        .ifM(F.suspend(sync)(logger.warn(msg, t)), F.unit)

    override def warn(msg: => String): F[Unit] =
      isWarnEnabled
        .ifM(F.suspend(sync)(logger.warn(msg)), F.unit)

    override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isWarnEnabled, ctx, () => logger.warn(msg))

    override def error(t: Throwable)(msg: => String): F[Unit] =
      isErrorEnabled
        .ifM(F.suspend(sync)(logger.error(msg, t)), F.unit)

    override def error(msg: => String): F[Unit] =
      isErrorEnabled
        .ifM(F.suspend(sync)(logger.error(msg)), F.unit)

    override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isErrorEnabled, ctx, () => logger.error(msg))

    override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(
        isTraceEnabled,
        ctx,
        { () =>
          logger.trace(msg)
          logger.trace(t)
        }
      )

    override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isDebugEnabled, ctx, () => logger.debug(msg, t))

    override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isInfoEnabled, ctx, () => logger.info(msg, t))

    override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isWarnEnabled, ctx, () => logger.warn(msg, t))

    override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isErrorEnabled, ctx, () => logger.error(msg, t))
  }
}
trait LoggerOps {
  private def makeLoggerFactory[F[_]: Sync] = new DiagnosticLoggerFactory[F] {
    override def getLoggerFromDiagnosticLogger(logger: jLogger): SelfAwareStructuredLogger[F] =
      DiagnosticLoggerInternal.DiagnosticLogger[F](logger)

    override def fromDiagnosticLogger(logger: jLogger): F[SelfAwareStructuredLogger[F]] =
      Sync[F].delay(getLoggerFromDiagnosticLogger(logger))

    override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
      DiagnosticLoggerInternal.DiagnosticLogger[F](jLogger.getInstance(name))

    override def fromName(name: String): F[SelfAwareStructuredLogger[F]] = Sync[F].delay(getLoggerFromName(name))
  }
  implicit val loggingIO: LoggerFactory[IO]           = makeLoggerFactory[IO]
//  implicit val loggingFactoryGenIO: LoggerFactoryGen[IO] = loggingIO
  implicit val loggingSynIO: LoggerFactory[SyncIO]       = makeLoggerFactory[SyncIO]
}
