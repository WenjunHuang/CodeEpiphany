package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.kernel.Async
import cats.syntax.all.*
import java.time.LocalDateTime
import org.jooq.impl.DSL
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.ChallengeRepository
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.utils.IdGenerator

object problemsets {
  def fetchAndUpdateProblemSets[F[_]: Async: LoggerFactory: HttpClientManager](project: Project): F[Unit] = {
    val api    = CodeForcesApi[F]()
    val logger = LoggerFactory.getLogger
    console.info[F](project, "Start to fetch problem sets of CodeForces ...") *>
      api.getAllProblemSets.flatMap { problems =>
        logger.info(s"Got ${problems.size} problems of CodeForces")
        ChallengeRepository
          .getInstance(project)
          .getDSLContextResource[F]
          .use { client =>
            Async[F].blocking {
              client.transactionResult {
                config =>
                  val dsl        = DSL.using(config)
                  val updateTime = LocalDateTime.now()
                  dsl
                    .deleteFrom(CODEFORCES_PROBLEMSETS)
                    .execute()
                  problems.foreach { case (problem, statistics) =>
                    dsl
                      .newRecord(CODEFORCES_PROBLEMSETS)
                      .setId(IdGenerator.nextId())
                      .setContestid(problem.contestId.map(long2Long).orNull)
                      .setIndex(problem.index)
                      .setContestidindex(s"${problem.contestId.map(_.toString).getOrElse("")}${problem.index}")
                      .setLastupdatedatetime(updateTime)
                      .setName(problem.name)
                      .setPoints(problem.points.map(float2Float).orNull)
                      .setProblemsetname(problem.problemsetName.orNull)
                      .setRating(problem.rating.map(int2Integer).orNull)
                      .setSolvedcount(statistics.solvedCount)
                      .setTags(problem.tags.mkString(","))
                      .setType(problem.`type`)
                      .store()
                  }
                problems.size
              }
            }
          }
      }.attempt.flatMap {
        case Right(count) =>
          console.info[F](project, s"Successfully fetch problem sets of CodeForces with $count problems")
        case Left(e) =>
          logger.warn(e)("Error to fetch problem sets of CodeForces") *>
            console.error[F](project, s"Error to fetch problem sets of CodeForces: \n ${e.getMessage}")
      }
  }
}
