package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.IO
import java.time.LocalDateTime
import org.jooq.impl.DSL
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.services.{ console, ChallengeRepository }
import com.wenjunhuang.codeepiphany.utils.IdGenerator
import com.wenjunhuang.codeepiphany.utils.syntax.*

object problemsets {
  def fetchAndUpdateProblemSets(project: Project): IO[Unit] = {
    val logger = LoggerFactory.getLogger[IO]
    console.info(project, CodeForces, PluginBundle.message("codeforces.problemsets.start")) *>
      CodeForcesApi.getAllProblemSets.flatMap { problems =>
        logger.info(s"Got ${problems.size} problems of CodeForces")
        ChallengeRepository
          .getInstance(project)
          .getDSLContextResource
          .use { client =>
            IO.blocking {
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
          console.info(project, CodeForces, PluginBundle.message("codeforces.problemsets.success", count))
        case Left(e) =>
          logger.warn(e)("Error to fetch problem sets of CodeForces") *>
            console.error(project, CodeForces, PluginBundle.message("codeforces.problemsets.error", e.getMessage))
      }
  }
}
