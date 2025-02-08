package com.wenjunhuang.codeepiphany.editor

import cats.effect.{ Async, Concurrent }
import cats.syntax.all.*
import org.typelevel.log4cats.{ Logger, LoggerFactory }

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.atcoder.services.AtCoderSubmissionService
import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesSubmissionService
import com.wenjunhuang.codeepiphany.hackerrank.services.{ HackerRankEvaluationService, HackerRankSubmissionService }
import com.wenjunhuang.codeepiphany.leetcode.services.{ LeetCodeEvaluationService, LeetCodeSubmissionService }
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.console.showConsole
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

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
              HackerRankEvaluationService[F](project).evaluateCode(vf, None)
            case CodeDojo.LeetCodeCN =>
              LeetCodeEvaluationService[F](project, CodeDojo.LeetCodeCN).evaluateCode(vf, None)
            case CodeDojo.LeetCode =>
              LeetCodeEvaluationService[F](project, CodeDojo.LeetCode).evaluateCode(vf, None)
            case CodeDojo.CodeForces | CodeDojo.AtCoder =>
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
        showConsole(project) *> console.info[F](project, s"Start to submit ${vf.getName} to ${item.dojo.show}") >>
          (
            item.dojo match
              case CodeDojo.HackerRank => HackerRankSubmissionService[F](project).submitCode(vf)
              case CodeDojo.LeetCodeCN => LeetCodeSubmissionService[F](project, CodeDojo.LeetCodeCN).submitCode(vf)
              case CodeDojo.LeetCode   => LeetCodeSubmissionService[F](project, CodeDojo.LeetCode).submitCode(vf)
              case CodeDojo.CodeForces => CodeForcesSubmissionService[F](project).submitCode(vf)
              case CodeDojo.AtCoder    => AtCoderSubmissionService[F](project).submitCode(vf)
          )
      case None => Async[F].unit
    ).handleErrorWith { e =>
      console.error[F](project, s"Error to submit code: \n ${e.getMessage}") >>
        logger.warn(e)("Error to submit code")
    }
  }

}
