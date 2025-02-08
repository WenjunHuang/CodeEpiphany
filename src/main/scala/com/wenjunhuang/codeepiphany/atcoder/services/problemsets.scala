package com.wenjunhuang.codeepiphany.atcoder.services

import cats.effect.implicits.*
import cats.effect.kernel.{ Async, Concurrent }
import cats.syntax.all.*
import org.jooq.impl.DSL
import org.typelevel.log4cats.LoggerFactory

import com.intellij.execution.filters.BrowserHyperlinkInfo
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderDifficulty
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.services.{ console, ChallengeRepository }
import com.wenjunhuang.codeepiphany.services.console.showConsole
import com.wenjunhuang.codeepiphany.services.console.MessageSeg.Hyperlink
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.utils.IdGenerator

object problemsets {
  def fetchAndUpdateProblemSets[F[_]: Async: Concurrent: LoggerFactory: HttpClientManager](
    project: Project
  ): F[Unit] = {
    val api    = AtCoderApi[F]()
    val logger = LoggerFactory.getLogger
    showConsole[F](project) *>
      console.info[F](
        project,
        "Start to fetch problem sets of AtCoder from ",
        Hyperlink("kenkoooo.com", BrowserHyperlinkInfo("https://kenkoooo.com/atcoder/"))
      ) *>
      (api.getAllProblemDifficulty, api.getAllProblems, api.getAllContests).parTupled.flatMap {
        case (difficulties, problems, contests) =>
          logger.info(s"Got ${problems.size} problems  and ${contests.size} contests of AtCoder")
          val contestsMap = contests.map { contest => contest.id -> contest.title }.toMap
          ChallengeRepository
            .getInstance(project)
            .getDSLContextResource[F]
            .use { client =>
              Async[F].blocking {
                client.transactionResult { config =>
                  val dsl = DSL.using(config)
                  dsl
                    .deleteFrom(ATCODER_PROBLEMS)
                    .execute()
                  problems.foreach { problem =>
                    dsl
                      .newRecord(ATCODER_PROBLEMS)
                      .setId(IdGenerator.nextId())
                      .setContestid(problem.contestId)
                      .setDifficulty(
                        difficulties
                          .get(problem.id)
                          .map(v => int2Integer(AtCoderDifficulty.calculateDisplayDifficulty(v)))
                          .orNull
                      )
                      .setFastestcontestid(problem.fastestContestId.orNull)
                      .setFastestsubmissionid(problem.fastestSubmissionId.map(long2Long).orNull)
                      .setFastestuserid(problem.fastestUserId.orNull)
                      .setFirstcontestid(problem.firstContestId.orNull)
                      .setFirstsubmissionid(problem.firstSubmissionId.map(long2Long).orNull)
                      .setFirstuserid(problem.firstUserId.orNull)
                      .setName(problem.name)
                      .setProblemid(problem.id)
                      .setProblemindex(problem.problemIndex)
                      .setShortestcontestid(problem.shortestContestId.orNull)
                      .setShortestsubmissionid(problem.shortestSubmissionId.map(long2Long).orNull)
                      .setShortestuserid(problem.shortestUserId.orNull)
                      .setSolvercount(problem.solverCount.map(int2Integer).orNull)
                      .setTitle(problem.title)
                      .setContesttitle(contestsMap.get(problem.contestId).orNull)
                      .store()
                  }

                  dsl
                    .deleteFrom(ATCODER_CONTESTS)
                    .execute()

                  contests.foreach { contest =>
                    dsl
                      .newRecord(ATCODER_CONTESTS)
                      .setId(IdGenerator.nextId())
                      .setContestid(contest.id)
                      .setDurationsecond(contest.durationSecond.map(long2Long).orNull)
                      .setRatechange(contest.rateChange.orNull)
                      .setStartepochsecond(contest.startEpochSecond.map(long2Long).orNull)
                      .setTitle(contest.title)
                      .store()
                  }
                  (problems.size, contests.size)
                }
              }
            }
      }.attempt.flatMap {
        case Right((problems, contests)) =>
          showConsole[F](project) *>
            console.info[F](project, s"Successfully fetch $problems problems and $contests contests of AtCoder")
        case Left(e) =>
          showConsole[F](project) *>
            console.error[F](project, s"Error to fetch problem sets of AtCoder: \n ${e.getMessage}")
            *> logger.warn(e)("Error to fetch problem sets of AtCoder")
      }
  }
}
