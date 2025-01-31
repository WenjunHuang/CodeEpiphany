package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.{ Async, Concurrent }
import cats.effect.implicits.*
import cats.syntax.all.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri
import org.http4s.Uri
import org.http4s.Method
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.codeforces.models.{
  CodeForcesChallengeData,
  CodeForcesProblem,
  CodeForcesProblemResponse,
  CodeForcesProblemStatistics
}
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager

trait CodeForcesApi[F[_]] {
  def getAllProblemSets: F[List[(CodeForcesProblem, CodeForcesProblemStatistics)]]
  def checkLogin(): F[Boolean]
  def getProblemTags: F[List[String]]

  def getChallengeData(
    problemsetName: Option[String],
    contestId: Long,
    index: String
  ): F[Option[CodeForcesChallengeData]]
}

object CodeForcesApi {

  def apply[F[_]: Async: Concurrent: HttpClientManager](): CodeForcesApi[F] = new CodeForcesApi[F]
    with Http4sClientDsl[F] {
    override def getAllProblemSets: F[List[(CodeForcesProblem, CodeForcesProblemStatistics)]] =
      HttpClientManager[F].getClient.use { client =>
        import org.http4s.circe.CirceEntityCodec.*

        (
          client.expect[CodeForcesProblemResponse](uri"https://codeforces.com/api/problemset.problems").map {
            response =>
              response.result.problems
                .zip(response.result.problemStatistics)
          },
          client
            .expect[CodeForcesProblemResponse](
              uri"https://codeforces.com/api/problemset.problems?problemsetName=acmsguru"
            )
            .map { response =>
              response.result.problems
                .map(_.copy(contestId = Some(99999)))
                .zip(response.result.problemStatistics)
            }
        ).parMapN(_ ++ _)
      }

    override def checkLogin(): F[Boolean] = {
      HttpClientManager[F].getClient.use { client =>
        client.get(uri"https://codeforces.com/settings/general") { response =>
          Async[F].delay { response.status.isSuccess }
        }
      }
    }

    override def getProblemTags: F[List[String]] = HttpClientManager[F].getClient.use { client =>
      client.expect[String](uri"https://codeforces.com/problemset").flatMap { content =>
        Async[F].delay {
          Jsoup.parse(content).select("label._FilterByTagsFrame_addTagLabel option").asScala.toList.collect {
            case elem if elem.hasAttr("value") && StringUtil.isNotEmpty(elem.attr("value")) => elem.attr("value")
          }
        }
      }
    }

    private def createChallengeDataUrl(problemsetName: Option[String], contestId: Long, index: String): Uri = {
      problemsetName match
        case None =>
          uri"https://codeforces.com/problemset/problem" / contestId.toString / index
        case Some(name) =>
          uri"https://codeforces.com/problemsets" / name / "problem" / contestId.toString / index
    }

    override def getChallengeData(
      problemsetName: Option[String],
      contestId: Long,
      index: String
    ): F[Option[CodeForcesChallengeData]] =
      HttpClientManager[F].getClient.use { client =>
        client.expect[String](Method.GET(createChallengeDataUrl(problemsetName, contestId, index))).map { content =>
          Jsoup.parse(content).select("div#pageContent div.ttypography").asScala.toList.headOption.map { element =>
            CodeForcesChallengeData(contestId = contestId, index = index, description = element.outerHtml())
          }
        }
      }
  }
}
