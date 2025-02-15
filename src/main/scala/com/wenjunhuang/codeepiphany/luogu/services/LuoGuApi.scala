package com.wenjunhuang.codeepiphany.luogu.services

import cats.effect.{ Async, Concurrent }
import cats.implicits.*
import cats.syntax.all.*
import io.circe.optics.JsonPath
import io.circe.parser.*
import java.net.HttpCookie
import org.http4s.{ Headers, Method, Uri }
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.jsoup.Jsoup
import fs2.Stream
import java.nio.ByteBuffer

import com.wenjunhuang.codeepiphany.luogu.models.{
  LuoGuChallengeItem,
  LuoGuDifficulty,
  LuoGuQuestionBank,
  LuoGuTag,
  LuoGuUserInfo
}
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager

trait LuoGuApi[F[_]] {
  def checkLogin(): F[Boolean]
  def getUserInfo: F[LuoGuUserInfo]
  def searchChallenges(
    difficulties: List[LuoGuDifficulty],
    luoguType: Option[LuoGuQuestionBank],
    tags: List[LuoGuTag],
    page: Int
  ): F[(Int, List[LuoGuChallengeItem])]

  def submitAnswer(pid: String, langId: String, code: String, captchaNeeded: ByteBuffer => F[String]): Stream[F, Int]
}

object LuoGuApi {
  def apply[F[_]: Async: Concurrent: HttpClientManager](): LuoGuApi[F] = new LuoGuApi[F] with Http4sClientDsl[F] {

    private def useClient[A](fun: Client[F] => F[A]): F[A] = HttpClientManager[F].getClient.use(fun)

    private def commonHeaders(csrfToken: String) =
      Headers("x-csrf-token" -> csrfToken, "x-requested-with" -> "XMLHttpRequest")

    private def getCSRFTokenAndPassAntiCrawler: F[String] =
      useClient { client =>
        client.expect[String](Uri.unsafeFromString(s"https://www.luogu.com.cn/")).flatMap { html =>
          val regex = """(?s:.*C3VK=(\w+);.*)""".r
          html match
            case regex(c3vk) =>
              // 防爬机制
              HttpClientManager[F].updateCookiesForHost(CodeDojo.LuoGu.domain, List(new HttpCookie("C3VK", c3vk)))
                *> getCSRFTokenAndPassAntiCrawler
            case _ =>
              Async[F].delay {
                val document = Jsoup.parse(html)
                document.select("meta[name=csrf-token]").attr("content")
              }
        }
      }

    override def checkLogin(): F[Boolean] = useClient { client =>
      getCSRFTokenAndPassAntiCrawler.flatMap { csrfToken =>
        client.expect[String](Uri.unsafeFromString("https://www.luogu.com.cn/user/setting")).map { html =>
          Jsoup.parse(html).select("title").text() == "用户设置 - 洛谷"
        }
      }
    }

    override def getUserInfo: F[LuoGuUserInfo] =
      getCSRFTokenAndPassAntiCrawler.flatMap { csrfToken =>
        useClient { client =>
          client
            .expect[String](Uri.unsafeFromString("https://www.luogu.com.cn/user/setting?_contentOnly=1"))
            .flatMap { html =>
              parse(html).flatMap { json =>
                (
                  JsonPath.root.currentData.user.name.string.getOption(json),
                  JsonPath.root.currentData.user.avatar.string.getOption(json)
                ).mapN((name, avatar) => LuoGuUserInfo(name, avatar)).toRight(new Exception("Failed to parse json"))
              }.liftTo[F]
            }
        }
      }

    override def searchChallenges(
      difficulties: List[LuoGuDifficulty],
      luoguType: Option[LuoGuQuestionBank],
      tags: List[LuoGuTag],
      page: Int
    ): F[(Int, List[LuoGuChallengeItem])] = useClient { client =>
      getCSRFTokenAndPassAntiCrawler.flatMap { csrfToken =>
        client
          .expect[String](
            Method.GET(
              uri"https://www.luogu.com.cn/problem/list"
                .withQueryParam("page", page)
                .withQueryParam("difficulty", difficulties.map(_.value).mkString(","))
                .withQueryParam("type", luoguType.map(_.value).getOrElse(""))
                .withQueryParam("_contentOnly", "1"),
              commonHeaders(csrfToken)
            )
          )
          .flatMap { content =>
            parse(content).flatMap { json =>
              (
                JsonPath.root.currentData.problems.count.int.getOption(json),
                JsonPath.root.currentData.problems.result.json.getOption(json)
              ).flatMapN { (count, problems) =>
                problems.as[List[LuoGuChallengeItem]].map((count, _)).toOption
              }.toRight(new Exception("Failed to parse json"))
            }.liftTo[F]
          }
      }
    }

    override def submitAnswer(
      pid: String,
      langId: String,
      code: String,
      captchaNeeded: ByteBuffer => F[String]
    ): Stream[F, Int] = ???
  }
}
