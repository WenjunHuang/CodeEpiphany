package com.wenjunhuang.codeepiphany.luogu.services

import cats.effect.{ Async, Concurrent, Temporal }
import cats.implicits.*
import cats.syntax.all.*
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import fs2.Stream
import io.circe.optics.JsonPath
import io.circe.parser.*
import io.circe.Json
import io.circe.syntax.*
import java.net.{ HttpCookie, URLDecoder }
import org.http4s.{ Headers, Method, Uri }
import org.http4s.client.{ Client, UnexpectedStatus }
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Referer
import org.http4s.implicits.uri
import org.jsoup.Jsoup
import scala.concurrent.duration.*
import scodec.bits.ByteVector
import scala.jdk.CollectionConverters.*

import com.wenjunhuang.codeepiphany.luogu.models.*
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettingsConfigurable.LUOGU_LANGUAGES_REVERSE
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, OrderDirection, SubmissionResult }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.utils.implicits.*

trait LuoGuApi[F[_]] {
  def checkLogin(): F[Boolean]
  def getUserInfo: F[LuoGuUserInfo]
  def searchChallenges(
    difficulties: Option[LuoGuDifficulty],
    luoguType: Option[LuoGuQuestionBank],
    tags: List[LuoGuTag],
    keyword: Option[String],
    orderBy: Option[(LuoGuSearchOrderBy, OrderDirection)],
    page: Int
  ): F[(Int, List[LuoGuChallengeItem])]

  def getChallengeData(pid: String): F[LuoGuChallengeData]

  def submitAnswer(
    pid: String,
    langId: String,
    code: String,
    captchaNeeded: ByteVector => F[String]
  ): Stream[F, LuoGuSubmissionResponse]
}

object LuoGuApi {
  def apply[F[_]: { Async, Concurrent, HttpClientManager }]: LuoGuApi[F] = new LuoGuApi[F] with Http4sClientDsl[F] {

    private def useClient[A](fun: Client[F] => F[A]): F[A] = HttpClientManager[F].getClient.use(fun)

    private def commonHeaders(csrfToken: String): Headers =
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
        client
          .expect[String](Uri.unsafeFromString("https://www.luogu.com.cn/user/setting"))
          .map { html =>
            val doc   = Jsoup.parse(html)
            val title = doc.select("title").text()
            if title == "用户设置 - 洛谷" || title == "Welcome - Luogu Spilopelia" then true
            else !doc.select("#app > h1").asScala.headOption.exists(_.text().contains("401"))
          }
          .handleErrorWith { case status: UnexpectedStatus =>
            Async[F].pure(false)
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
              val doc     = Jsoup.parse(html)
              val jsonStr = doc.select("#lentille\\-context").html()
              parse(jsonStr).flatMap { json =>
                (JsonPath.root.user.name.string.getOption(json), JsonPath.root.user.avatar.string.getOption(json))
                  .mapN((name, avatar) => LuoGuUserInfo(name, avatar))
                  .toRight(new Exception("Failed to parse json"))
              }.liftTo[F]
            }
        }
      }

    override def searchChallenges(
      difficulties: Option[LuoGuDifficulty],
      luoguType: Option[LuoGuQuestionBank],
      tags: List[LuoGuTag],
      keyword: Option[String],
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
                  .withQueryParam("keyword", keyword.getOrElse(""))
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

    private def postAnswer(
      pid: String,
      langId: String,
      code: String,
      captcha: Option[String],
      captchaNeeded: ByteVector => F[String]
    ): F[String] = {
      getCSRFTokenAndPassAntiCrawler.flatMap { csrfToken =>
        useClient { client =>
          import org.http4s.circe.CirceEntityEncoder.*
          client
            .run(
              Method
                .POST(
                  uri"https://www.luogu.com.cn/fe/api/problem/submit" / pid,
                  commonHeaders(csrfToken).put(Referer(uri"https://www.luogu.com.cn/problem" / pid))
                )
                .withEntity(
                  (Map("lang" -> langId.toInt.asJson, "enableO2" -> 1.asJson, "code" -> code.asJson) ++ captcha
                    .map("captcha" -> _.asJson)
                    .toMap).asJson
                )
            )
            .use { response =>
              response.status.code match
                case 200 =>
                  response.as[String].flatMap { body =>
                    parse(body).flatMap { json =>
                      JsonPath.root.rid.number
                        .getOption(json)
                        .map(_.toString)
                        .toRight(new Exception("Failed to parse json"))
                    }.liftTo[F]
                  }
                case 403 =>
                  client
                    .get[ByteVector](
                      uri"https://www.luogu.com.cn/api/verify/captcha"
                        .withQueryParam("_t", s"${System.currentTimeMillis()}")
                    ) { response =>
                      response.contentType match
                        case Some(contentType) if contentType.mediaType.isImage =>
                          response.body.compile.to(ByteVector)
                        case _ => Async[F].raiseError(new Exception("Failed to get captcha"))
                    }
                    .flatMap { captchaImage =>
                      captchaNeeded(captchaImage).flatMap { captcha =>
                        postAnswer(pid, langId, code, Some(captcha), captchaNeeded)
                      }
                    }
                case _ =>
                  response
                    .as[String]
                    .flatMap { body =>
                      parse(body).flatMap { json =>
                        JsonPath.root.errorMessage.string.getOption(json).toRight(new Exception("Failed to parse json"))
                      }.liftTo[F]
                    }
                    .recoverWith(_ => Async[F].pure(response.status.code.toString))
                    .flatMap { msg =>
                      Async[F].raiseError(new Exception(s"Failed to submit answer: $msg"))
                    }
            }
        }
      }
    }

    private def getSubmitAnswerResult(oldResponse: LuoGuSubmissionResponse): F[LuoGuSubmissionResponse] = {
      useClient { client =>
        client
          .expect[String](uri"https://www.luogu.com.cn/record" / oldResponse.submissionId)
          .flatMap { html =>
            val regex = """(?s:.*decodeURIComponent\("(.*)"\).*)""".r
            html match
              case regex(encoded) =>
                parse(URLDecoder.decode(encoded, "UTF-8")).map { json =>
                  JsonPath.root.currentData.record.status.int
                    .getOption(json)
                    .map { status =>
                      val submissionResult = luoguSubmissionStatus(status) match
                        case r @ SubmissionResult.Failure =>
                          val statuses =
                            JsonPath.root.currentData.record.detail.judgeResult.subtasks.each.testCases.each.status.int
                              .getAll(json)
                          statuses
                            .map(luoguSubmissionStatus)
                            .find(it =>
                              it != SubmissionResult.Success && it != SubmissionResult.Processing && it != SubmissionResult.Failure && it != SubmissionResult.Unknown
                            )
                            .getOrElse(r)
                        case r => r
                      val msg = submissionResult match
                        case SubmissionResult.CompilationError =>
                          JsonPath.root.currentData.record.detail.compileResult.message.string
                            .getOption(json)
                            .getOrElse("")
                        case _ => ""
                      LuoGuSubmissionResponse(oldResponse.submissionId, submissionResult, msg)
                    }
                    .getOrElse(oldResponse)
                }.liftTo[F]
              case _ => Async[F].raiseError(new Exception("Failed to parse submission result"))
          }
          .recoverWith {
            case e: UnexpectedStatus if e.status.code == 503 =>
              Temporal[F].sleep(2.second) >> getSubmitAnswerResult(oldResponse)
          }
      }
    }

    override def submitAnswer(
      pid: String,
      langId: String,
      code: String,
      captchaNeeded: ByteVector => F[String]
    ): Stream[F, LuoGuSubmissionResponse] =
      Stream
        .eval(postAnswer(pid, langId, code, None, captchaNeeded))
        .flatMap { submissionId =>
          Stream
            .repeatEval(Temporal[F].sleep(2.second))
            .evalScan(LuoGuSubmissionResponse(submissionId, SubmissionResult.Processing, "")) { (lastResponse, _) =>
              getSubmitAnswerResult(lastResponse).retryLimitsWithBackoff(5, 2.seconds)
            }
            .flatMap { response =>
              response.result match
                case SubmissionResult.Processing => Stream(Option(response).widen)
                case _                           => Stream(Option(response).widen, None)
            }
            .unNoneTerminate
        }
  }

  private def luoguSubmissionStatus(status: Int): SubmissionResult = status match
    case 0  => SubmissionResult.Processing
    case 1  => SubmissionResult.Processing
    case 2  => SubmissionResult.CompilationError
    case 3  => SubmissionResult.OutputLimitExceeded
    case 4  => SubmissionResult.MemoryLimitExceeded
    case 5  => SubmissionResult.Timeout
    case 7  => SubmissionResult.RuntimeError
    case 12 => SubmissionResult.Success
    case _  => SubmissionResult.Failure
}
