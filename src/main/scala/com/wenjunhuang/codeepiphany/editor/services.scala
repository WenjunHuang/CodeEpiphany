package com.wenjunhuang.codeepiphany.editor

import cats.effect.{ Concurrent, IO }
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.wenjunhuang.codeepiphany.atcoder.services.AtCoderSubmissionService
import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesSubmissionService
import com.wenjunhuang.codeepiphany.hackerrank.services.{ HackerRankEvaluationService, HackerRankSubmissionService }
import com.wenjunhuang.codeepiphany.leetcode.services.{ LeetCodeEvaluationService, LeetCodeSubmissionService }
import com.wenjunhuang.codeepiphany.luogu.services.LuoGuSubmissionService
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.console.showConsole
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.syntax.*
import org.typelevel.log4cats.{ Logger, LoggerFactory }
import com.wenjunhuang.codeepiphany.PluginBundle

object services {
  def runCode(vf: VirtualFile, project: Project): IO[Unit] = {
    showConsole(project)
      *> console.info(project, PluginBundle.message("run.code.start", vf.getName))
      *> IO.delay {
        val settings = ChallengeSettings.getInstance(project)
        settings.findChallengeId(vf)
      }.flatMap {
        case Some(item) =>
          item.dojo match
            case CodeDojo.HackerRank =>
              HackerRankEvaluationService(project).evaluateCode(vf, None)
            case CodeDojo.LeetCodeCN =>
              LeetCodeEvaluationService(project, CodeDojo.LeetCodeCN).evaluateCode(vf, None)
            case CodeDojo.LeetCode =>
              LeetCodeEvaluationService(project, CodeDojo.LeetCode).evaluateCode(vf, None)
            case CodeDojo.CodeForces | CodeDojo.AtCoder | CodeDojo.LuoGu =>
              IO.unit
        case None => IO.unit
      }.handleErrorWith { e =>
        console.error(project, PluginBundle.message("error.run.code", e.getMessage))
      }
  }

  def submitCode(vf: VirtualFile, project: Project): IO[Unit] = {
    val logger   = LoggerFactory.getLogger[IO]
    val settings = ChallengeSettings.getInstance(project)
    (settings.findChallengeId(vf) match
      case Some(item) =>
        showConsole(project) *> console.info(
          project,
          PluginBundle.message("submit.code.start", vf.getName, item.dojo.show)
        ) >>
          (
            item.dojo match
              case CodeDojo.HackerRank => HackerRankSubmissionService(project).submitCode(vf)
              case CodeDojo.LeetCodeCN => LeetCodeSubmissionService(project, CodeDojo.LeetCodeCN).submitCode(vf)
              case CodeDojo.LeetCode   => LeetCodeSubmissionService(project, CodeDojo.LeetCode).submitCode(vf)
              case CodeDojo.CodeForces => CodeForcesSubmissionService(project).submitCode(vf)
              case CodeDojo.AtCoder    => AtCoderSubmissionService(project).submitCode(vf)
              case CodeDojo.LuoGu      => LuoGuSubmissionService(project).submitCode(vf)
          )
      case None => IO.unit
    ).handleErrorWith { e =>
      console.error(project, PluginBundle.message("error.submit.code", e.getMessage)) >>
        logger.warn(e)("Error to submit code")
    }
  }
}
