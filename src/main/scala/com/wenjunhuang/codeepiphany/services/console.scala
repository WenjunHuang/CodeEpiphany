package com.wenjunhuang.codeepiphany.services

import cats.effect.{Async, Concurrent}
import cats.syntax.all.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.typelevel.log4cats.LoggerFactory

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.{LogConsoleView, SidebarWindowFactory}
import com.wenjunhuang.codeepiphany.utils.syntax.*

object console {
  def info[F[_]: Async](project: Project, messages: MessageSeg*): F[Unit] =
    print(project, ConsoleViewContentType.LOG_INFO_OUTPUT, messages*)

  def warn[F[_]: Async](project: Project, messages: MessageSeg*): F[Unit] =
    print(project, ConsoleViewContentType.LOG_WARNING_OUTPUT, messages*)

  def error[F[_]: Async](project: Project, messages: MessageSeg*): F[Unit] =
    print(project, ConsoleViewContentType.LOG_ERROR_OUTPUT, messages*)

  private def print[F[_]: Async](project: Project, cvct: ConsoleViewContentType, messages: MessageSeg*): F[Unit] =
    LogConsoleView.getConsoleViewF(project).map { consoleView =>
      consoleView.requestScrollingToEnd()
      cvct match
        case ConsoleViewContentType.LOG_ERROR_OUTPUT   => consoleView.print(s"❌ ${currentDateTime()}\n", cvct)
        case ConsoleViewContentType.LOG_WARNING_OUTPUT => consoleView.print(s"⚠️ ${currentDateTime()}\n", cvct)
        case _                                         => consoleView.print(s"ℹ️ ${currentDateTime()}\n", cvct)

      messages.foreach {
        case MessageSeg.Str(msg)                  => consoleView.print(msg, cvct)
        case MessageSeg.Hyperlink(msg, hyperlink) => consoleView.printHyperlink(msg, hyperlink)
      }
      consoleView.print("\n", cvct)
    }

  private def currentDateTime(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

  def showConsole[F[_]: {Async, Concurrent, HttpClientManager, LoggerFactory}](project: Project): F[Unit] = {
    Async[F].delay { SidebarWindowFactory.activate(project, LogConsoleView.DISPLAY_NAME) }.evalOnEDTDefault()
  }

  enum MessageSeg:
    case Str(msg: String)
    case Hyperlink(msg: String, hyperlink: HyperlinkInfo)

  implicit def strToMessageSeg(msg: String): MessageSeg = MessageSeg.Str(msg)
}
