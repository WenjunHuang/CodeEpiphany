package com.wenjunhuang.codeepiphany.editor

import cats.effect.{ Async, Concurrent }
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.{ LogConsoleView, SidebarWindowFactory }
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.typelevel.log4cats.LoggerFactory

package object services {
  def runCode[F[_]: Async: Concurrent: HttpClientKeeper](vf: VirtualFile, project: Project): F[Unit] = {
    val settings = ChallengeSettings.getInstance(project)
    settings.findChallengeId(vf) match
      case Some(item) =>
        item.dojo match
          case CodeDojo.HackerRank =>
            Async[F].delay { SidebarWindowFactory.activate(project, LogConsoleView.DISPLAY_NAME) }
              .evalOnEDTDefault()
              *> console.info[F](project, s"Start to run ${vf.getName}")
              *> hackerrank.runCode[F](vf, project, item)
          case _ => Async[F].unit
      case None => Async[F].unit
  }

  def submitCode[F[_]: Async: Concurrent: HttpClientKeeper: LoggerFactory](
    vf: VirtualFile,
    project: Project
  ): F[Unit] = {
    val logger   = LoggerFactory[F].getLogger
    val settings = ChallengeSettings.getInstance(project)
    (settings.findChallengeId(vf) match
      case Some(item) =>
        item.dojo match
          case CodeDojo.HackerRank =>
            Async[F].delay { SidebarWindowFactory.activate(project, LogConsoleView.DISPLAY_NAME) }
              .evalOnEDTDefault()
              *> console.info[F](project, s"Start to submit ${vf.getName} to HackerRank")
              *> hackerrank.submitCode[F](vf, project, item)
          case _ => Async[F].unit
      case None => Async[F].unit
    ).handleErrorWith { e =>
      logger.warn(e)("Error to submit code") *>
        console.error[F](project, s"Error to submit code: \n ${e.getMessage}")
    }
  }
}
