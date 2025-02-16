package com.wenjunhuang.codeepiphany.luogu.services

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
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
import io.circe.Json
import java.nio.ByteBuffer

import com.wenjunhuang.codeepiphany.luogu.models.{
  LuoGuChallengeData,
  LuoGuChallengeItem,
  LuoGuDifficulty,
  LuoGuQuestionBank,
  LuoGuSearchOrderBy,
  LuoGuTag,
  LuoGuUserInfo
}
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettingsConfigurable.LUOGU_LANGUAGES_REVERSE
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, OrderDirection }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager

trait LuoGuApi[F[_]] {
  def checkLogin(): F[Boolean]
  def getUserInfo: F[LuoGuUserInfo]
  def searchChallenges(
    difficulties: Option[LuoGuDifficulty],
    luoguType: Option[LuoGuQuestionBank],
    tags: List[LuoGuTag],
    orderBy: Option[(LuoGuSearchOrderBy, OrderDirection)],
    page: Int
  ): F[(Int, List[LuoGuChallengeItem])]

  def getChallengeData(pid: String): F[LuoGuChallengeData]
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

    private def makeDescriptionHtml(json: Json): String = {

      val title =
        s"# ${JsonPath.root.data.problem.pid.string.getOption(json).getOrElse("")} ${JsonPath.root.data.problem.title.string.getOption(json).getOrElse("")}"
      val description =
        s"## 题目描述\n\n${JsonPath.root.data.problem.content.description.string.getOption(json).getOrElse("")}"
      val formatI = s"## 输入格式\n\n${JsonPath.root.data.problem.content.formatI.string.getOption(json).getOrElse("")}"
      val formatO = s"## 输出格式\n\n${JsonPath.root.data.problem.content.formatO.string.getOption(json).getOrElse("")}"
      val hint    = s"## 说明/提示\n\n${JsonPath.root.data.problem.content.hint.string.getOption(json).getOrElse("")}"
      val translation = JsonPath.root.data.problem.translation.string.getOption(json).getOrElse("") match {
        case "" => ""
        case t  => s"## 题意翻译\n\n$t"
      }
      val limits = JsonPath.root.data.problem.limits.time.arr
        .getOption(json)
        .map(_.toList)
        .zip(JsonPath.root.data.problem.limits.memory.arr.getOption(json).map(_.toList))
        .map(it => it._1.zip(it._2))
        .getOrElse(List.empty) match {
        case (time, memory) :: _ =>
          s"## 时间限制\n\n${time.asNumber.map(_.toDouble / 1000).getOrElse(0)}s\n\n## 内存限制\n\n${memory.asNumber.map(_.toDouble / 1024).getOrElse(0)}MB"
        case _ => ""
      }

      val samples =
        JsonPath.root.data.problem.samples.arr
          .getOption(json)
          .map(_.toList)
          .getOrElse(List.empty)
          .zipWithIndex
          .map { case (sample, index) =>
            sample.as[List[String]].getOrElse(List.empty) match
              case input :: output :: Nil =>
                s"## 样例${index + 1}\n\n#### 输入\n\n```\n$input\n```\n\n#### 输出\n\n```\n$output\n```"
              case _ => ""
          }
          .mkString("\n")
      val markdown = s"$title\n\n$limits\n\n$description\n\n$formatI\n\n$formatO\n\n$translation\n\n$samples\n\n$hint"
      val options  = new MutableDataSet()
      val parser   = Parser.builder(options).build()
      val renderer = HtmlRenderer.builder(options).build()
      val document = parser.parse(markdown)
      renderer.render(document)
    }

    override def getChallengeData(pid: String): F[LuoGuChallengeData] =
      getCSRFTokenAndPassAntiCrawler.flatMap { csrfToken =>
        useClient { client =>
          client
            .expect[String](Method.GET(uri"https://www.luogu.com.cn/problem" / pid, commonHeaders(csrfToken)))
            .flatMap { html =>
              val doc         = Jsoup.parse(html)
              val jsonContent = doc.select("script#lentille-context[type=\"application/json\"]").html()
              parse(jsonContent).flatMap { json =>
                val description = makeDescriptionHtml(json)
                (
                  JsonPath.root.data.problem.pid.string.getOption(json),
                  JsonPath.root.data.problem.title.string.getOption(json),
                  JsonPath.root.data.problem.acceptLanguages.as[List[Int]].getOption(json)
                ).mapN { (pid, title, acceptLanguages) =>
                  LuoGuChallengeData(
                    pid,
                    title,
                    description,
                    acceptLanguages
                      .map(_.toString)
                      .collect {
                        case v if LUOGU_LANGUAGES_REVERSE.contains(v) => LUOGU_LANGUAGES_REVERSE(v)
                      }
                      .toSet
                  )
                }.toRight(new Exception("Failed to parse challenge data"))
              }.liftTo[F]
            }
        }
      }

    override def getUserInfo: F[LuoGuUserInfo] =
      getCSRFTokenAndPassAntiCrawler.flatMap { csrfToken =>
        useClient { client =>
          client
            .expect[String](
              Method.GET(
                Uri.unsafeFromString("https://www.luogu.com.cn/user/setting?_contentOnly=1"),
                commonHeaders(csrfToken)
              )
            )
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
      difficulties: Option[LuoGuDifficulty],
      luoguType: Option[LuoGuQuestionBank],
      tags: List[LuoGuTag],
      orderBy: Option[(LuoGuSearchOrderBy, OrderDirection)],
      page: Int
    ): F[(Int, List[LuoGuChallengeItem])] = useClient { client =>
      getCSRFTokenAndPassAntiCrawler.flatMap { csrfToken =>
        client
          .expect[String](
            Method.GET(
              orderBy.foldLeft(
                uri"https://www.luogu.com.cn/problem/list"
                  .withQueryParam("page", page)
                  .withQueryParam("difficulty", difficulties.map(_.value.toString).getOrElse(""))
                  .withQueryParam("type", luoguType.map(_.value).getOrElse(""))
                  .withQueryParam("tag", tags.map(_.id).mkString(","))
                  .withQueryParam("_contentOnly", "1")
              ) { case (uri, (orderBy, direction)) => orderBy.createOrderBy(uri, direction) },
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
