package com.wenjunhuang.codeepiphany.atcoder.services

import cats.effect.kernel.Async
import cats.effect.Concurrent
import cats.syntax.all.*
import fs2.Stream
import io.circe.JsonObject
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.atcoder.models.{AtCoderChallengeData, AtCoderContest, AtCoderProblem, AtCoderSubmissionResponse, AtCoderUserInfo}
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettingsConfigurable.ATCODER_LANGUAGES_REVERSE
import com.wenjunhuang.codeepiphany.model.{ApiError, CodeDojo}
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager

trait AtCoderApi[F[_]] {
  def checkLogin(): F[Boolean]
  def getUserInfo: F[AtCoderUserInfo]
  def getAllProblems: F[List[AtCoderProblem]]
  def getAllContests: F[List[AtCoderContest]]
  def getAllProblemDifficulty: F[Map[String, Int]]
  def getChallengeData(contestId: String, problemId: String): F[Option[AtCoderChallengeData]]
  def submitAnswer(problemId: String, languageId: String, code: String): Stream[F, AtCoderSubmissionResponse]
}

object AtCoderApi {
  def apply[F[_]: Async: Concurrent: HttpClientManager](): AtCoderApi[F] = new AtCoderApi[F] with Http4sClientDsl[F] {
    override def checkLogin(): F[Boolean] = useClient { client =>
      client.expect[String](uri"https://atcoder.jp/settings").map { content =>
        !Jsoup.parse(content).select("#main-container #user-nav-tabs").isEmpty
      }
    }

    override def getUserInfo: F[AtCoderUserInfo] = useClient { client =>
      client.expect[String](uri"https://atcoder.jp/settings").flatMap { content =>
        Jsoup
          .parse(content)
          .select("input[id='ui.UserName']")
          .asScala
          .headOption
          .flatMap { element =>
            val username = element.attr("value")
            if StringUtil.isEmpty(username) then None
            else Some(username)
          }
          .map { username =>
            client
              .expect[String](uri"https://atcoder.jp/users" / username)
              .map { html =>
                Jsoup
                  .parse(html)
                  .select("img.avatar")
                  .asScala
                  .headOption
                  .flatMap { element =>
                    val avatarUrl = element.attr("src")
                    if StringUtil.isEmpty(avatarUrl) then None
                    else Some(avatarUrl)
                  }
                  .getOrElse("")
              }
              .map { avatarUrl =>
                AtCoderUserInfo(username, avatarUrl)
              }
          }
          .traverse(identity)
          .map(_.getOrElse(throw ApiError.NotFound(CodeDojo.AtCoder, "User info not found")))
      }
    }

    override def getAllProblems: F[List[AtCoderProblem]] = useClient { client =>
      import org.http4s.circe.CirceEntityCodec.*
      client.expect[List[AtCoderProblem]](uri"https://kenkoooo.com/atcoder/resources/merged-problems.json")
    }

    override def getAllContests: F[List[AtCoderContest]] = useClient { client =>
      import org.http4s.circe.CirceEntityCodec.*
      client.expect[List[AtCoderContest]](uri"https://kenkoooo.com/atcoder/resources/contests.json")
    }

    override def getChallengeData(contestId: String, problemId: String): F[Option[AtCoderChallengeData]] = useClient {
      client =>
        client
          .expect[String](uri"https://atcoder.jp/contests" / contestId / "tasks " / problemId)
          .map { html =>
            val doc = Jsoup.parse(html)
            doc.select("div#main-container div.col-sm-12").asScala.headOption.map(it => it.html()).map { description =>
              val supportedLanguages = doc
                .select("select[name='data.LanguageId'] option")
                .asScala
                .collect {
                  case element if StringUtil.isNotEmpty(element.attr("value")) => element.attr("value")
                }
                .map { value =>
                  ATCODER_LANGUAGES_REVERSE.get(value)
                }
                .collect { case Some(v) =>
                  v
                }
                .toList
              AtCoderChallengeData(contestId, problemId, description, supportedLanguages.toSet)
            }
          }
    }

    override def getAllProblemDifficulty: F[Map[String, Int]] = useClient { client =>
      client.expect[String](uri"https://kenkoooo.com/atcoder/resources/problem-models.json").map { json =>
        import io.circe.parser.decode
        decode[Map[String, JsonObject]](json)
          .map(_.view.mapValues(_.apply("difficulty").flatMap(_.asNumber).flatMap(_.toInt).getOrElse(0)).toMap)
          .getOrElse(Map.empty[String, Int])
      }
    }

    override def submitAnswer(
      problemId: String,
      languageId: String,
      code: String
    ): Stream[F, AtCoderSubmissionResponse] = ???

    private def useClient[A](f: Client[F] => F[A]): F[A] = HttpClientManager[F].getClient.use(f)
  }
}
