package com.wenjunhuang.codeepiphany.services

import cats.effect.IO
import cats.syntax.all.*

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.toolwindows.sidebar.{ LogConsoleView, SidebarWindowFactory }
import com.wenjunhuang.codeepiphany.utils.syntax.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.wenjunhuang.codeepiphany.model.CodeDojo

object console {
  def info(project: Project, codeDojo: CodeDojo, messages: MessageSeg*): IO[Unit] =
    print(project, ConsoleViewContentType.LOG_INFO_OUTPUT, codeDojo.some, messages*)

  def info(project: Project, messages: MessageSeg*): IO[Unit] =
    print(project, ConsoleViewContentType.LOG_INFO_OUTPUT, None, messages*)

  def warn(project: Project, codeDojo: CodeDojo, messages: MessageSeg*): IO[Unit] =
    print(project, ConsoleViewContentType.LOG_WARNING_OUTPUT, codeDojo.some, messages*)

  def warn(project: Project, messages: MessageSeg*): IO[Unit] =
    print(project, ConsoleViewContentType.LOG_WARNING_OUTPUT, None, messages*)

  def error(project: Project, codeDojo: CodeDojo, messages: MessageSeg*): IO[Unit] =
    print(project, ConsoleViewContentType.LOG_ERROR_OUTPUT, codeDojo.some, messages*)

  def error(project: Project, messages: MessageSeg*): IO[Unit] =
    print(project, ConsoleViewContentType.LOG_ERROR_OUTPUT, None, messages*)

  private def print(
    project: Project,
    cvct: ConsoleViewContentType,
    codeDojo: Option[CodeDojo],
    messages: MessageSeg*
  ): IO[Unit] =
    LogConsoleView.getConsoleViewF(project).map { consoleView =>
      consoleView.requestScrollingToEnd()
      cvct match
        case ConsoleViewContentType.LOG_ERROR_OUTPUT =>
          consoleView.print(s"❌ ${codeDojo.map(cd => s"[${cd.show}] ").getOrElse("")}${currentDateTime()}\n", cvct)
        case ConsoleViewContentType.LOG_WARNING_OUTPUT =>
          consoleView.print(s"⚠️ ${codeDojo.map(cd => s"[${cd.show}] ").getOrElse("")}${currentDateTime()}\n", cvct)
        case _ =>
          consoleView.print(s"ℹ️ ${codeDojo.map(cd => s"[${cd.show}] ").getOrElse("")}${currentDateTime()}\n", cvct)

      messages.foreach {
        case MessageSeg.Str(msg)                  => consoleView.print(msg, cvct)
        case MessageSeg.Hyperlink(msg, hyperlink) => consoleView.printHyperlink(msg, hyperlink)
      }
      consoleView.print("\n", cvct)
    }

  private def currentDateTime(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

  def showConsole(project: Project): IO[Unit] = {
    IO.delay { SidebarWindowFactory.activate(project, LogConsoleView.DISPLAY_NAME) }.evalOnEDTDefault()
  }

  enum MessageSeg:
    case Str(msg: String)
    case Hyperlink(msg: String, hyperlink: HyperlinkInfo)

  implicit def strToMessageSeg(msg: String): MessageSeg = MessageSeg.Str(msg)
}
