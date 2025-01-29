package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.{Async, Concurrent}
import cats.syntax.all.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri

import com.wenjunhuang.codeepiphany.codeforces.models.{CodeForcesProblem, CodeForcesProblemResponse, CodeForcesProblemStatistics}
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager

trait CodeForcesApi[F[_]] {
  def getAllProblemSets: F[List[(CodeForcesProblem, CodeForcesProblemStatistics)]]
}

object CodeForcesApi {

  def apply[F[_]: Async: Concurrent: HttpClientManager](): CodeForcesApi[F] = new CodeForcesApi[F]
    with Http4sClientDsl[F] {
    override def getAllProblemSets: F[List[(CodeForcesProblem, CodeForcesProblemStatistics)]] =
      HttpClientManager[F].getClient.use { client =>
        client.expect[CodeForcesProblemResponse](uri"https://codeforces.com/api/problemset.problems").map { response =>
          response.result.problems
            .zip(response.result.problemStatistics)
        }
      }
  }
}
