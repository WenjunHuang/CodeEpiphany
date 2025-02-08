package com.wenjunhuang.codeepiphany.atcoder.services

import cats.effect.kernel.Async
import cats.effect.Concurrent
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri

import com.wenjunhuang.codeepiphany.atcoder.models.{ AtCoderContest, AtCoderProblem }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager

trait AtCoderApi[F[_]] {
  def getAllProblems: F[List[AtCoderProblem]]
  def getAllContests: F[List[AtCoderContest]]
}

object AtCoderApi {
  def apply[F[_]: Async: Concurrent: HttpClientManager](): AtCoderApi[F] = new AtCoderApi[F] with Http4sClientDsl[F] {
    override def getAllProblems: F[List[AtCoderProblem]] = useClient { client =>
      import org.http4s.circe.CirceEntityCodec.*
      client.expect[List[AtCoderProblem]](uri"https://kenkoooo.com/atcoder/resources/merged-problems.json")
    }

    override def getAllContests: F[List[AtCoderContest]] = useClient { client =>
      import org.http4s.circe.CirceEntityCodec.*
      client.expect[List[AtCoderContest]](uri"https://kenkoooo.com/atcoder/resources/contests.json")
    }

    private def useClient[A](f: Client[F] => F[A]): F[A] = HttpClientManager[F].getClient.use(f)
  }
}
