package com.wenjunhuang.codeepiphany.editor

import cats.effect.{ Async, Concurrent }
import cats.syntax.all.*
import org.typelevel.log4cats.{ Logger, LoggerFactory, SelfAwareStructuredLogger }

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.{ LogConsoleView, SidebarWindowFactory }
import com.wenjunhuang.codeepiphany.utils.implicits.*

package object services {
  def runCode[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](vf: VirtualFile, project: Project): F[Unit] = {
    showConsole(project)
      *> console.info[F](project, s"Start to run ${vf.getName}")
      *> Async[F].delay {
        val settings = ChallengeSettings.getInstance(project)
        settings.findChallengeId(vf)
      }.flatMap {
        case Some(item) =>
          item.dojo match
            case CodeDojo.HackerRank =>
              HackerRankService[F]().runCode(vf, project, item)
            case CodeDojo.LeetCodeCN =>
              LeetCodeCodeEvaluationService[F](project, CodeDojo.LeetCodeCN).evaluateCode(vf, None)
            case CodeDojo.LeetCode =>
              LeetCodeService[F]().runCode(vf, project, item)
            case CodeDojo.CodeForces =>
              Async[F].unit
        case None => Async[F].unit
      }.handleErrorWith { e =>
        console.error[F](project, s"Error to run code: \n ${e.getMessage}")
      }
  }

  def submitCode[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](
    vf: VirtualFile,
    project: Project
  ): F[Unit] = {
    implicit val logger: Logger[F] = LoggerFactory[F].getLogger
    val settings                   = ChallengeSettings.getInstance(project)
    (settings.findChallengeId(vf) match
      case Some(item) =>
        showConsole(project) *>
          console.info[F](project, s"Start to submit ${vf.getName} to ${item.dojo.show}") *>
          (
            item.dojo match
              case CodeDojo.HackerRank => HackerRankService[F]().submitCode(vf, project, item)
              case CodeDojo.LeetCodeCN => LeetCodeService[F]().submitCode(vf, project, item)
              case CodeDojo.LeetCode   => LeetCodeService[F]().submitCode(vf, project, item)
              case CodeDojo.CodeForces => CodeForcesService[F]().submitCode(vf, project, item)
          )
      case None => Async[F].unit
    ).handleErrorWith { e =>
      logger.warn(e)("Error to submit code") *> console.error[F](project, s"Error to submit code: \n ${e.getMessage}")
    }
  }

  private def showConsole[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](project: Project) = {
    Async[F].delay { SidebarWindowFactory.activate(project, LogConsoleView.DISPLAY_NAME) }.evalOnEDTDefault()
  }
}
