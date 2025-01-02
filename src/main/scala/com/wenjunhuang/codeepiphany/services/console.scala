package com.wenjunhuang.codeepiphany.services

import cats.syntax.all.*
import cats.effect.Async
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.LogConsoleView
import com.wenjunhuang.codeepiphany.utils.implicits.*

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object console {
  def info[F[_]: Async](project: Project, message: String): F[Unit] =
    print(project, message, ConsoleViewContentType.LOG_INFO_OUTPUT)

  def warn[F[_]: Async](project: Project, message: String): F[Unit] =
    print(project, message, ConsoleViewContentType.LOG_WARNING_OUTPUT)

  def error[F[_]: Async](project: Project, message: String): F[Unit] =
    print(project, message, ConsoleViewContentType.LOG_ERROR_OUTPUT)

  private def print[F[_]: Async](project: Project, message: String, cvct: ConsoleViewContentType): F[Unit] =
    LogConsoleView.getConsoleViewF(project).map { consoleView =>
      consoleView.requestScrollingToEnd()
      cvct match
        case ConsoleViewContentType.LOG_ERROR_OUTPUT   => consoleView.print(s"❌ ${currentDateTime()}\n", cvct)
        case ConsoleViewContentType.LOG_WARNING_OUTPUT => consoleView.print(s"⚠️ ${currentDateTime()}\n", cvct)
        case _                                         => consoleView.print(s"ℹ️ ${currentDateTime()}\n", cvct)
      consoleView.print(message + "\n", cvct)
    }

  private def currentDateTime(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

}
