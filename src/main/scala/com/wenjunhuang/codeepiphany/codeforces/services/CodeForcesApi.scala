package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.{ Async, Concurrent, Temporal }
import cats.effect.implicits.*
import cats.syntax.all.*
import fs2.Stream
import io.circe.optics.JsonPath
import io.circe.parser.parse
import java.time.{ LocalDateTime, ZoneId }
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.http4s.{ Headers, Method, Uri, UrlForm }
import org.http4s.client.{ Client, UnexpectedStatus }
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{ Accept, Referer }
import org.http4s.implicits.uri
import org.jsoup.Jsoup
import org.typelevel.ci.CIString
import retry.*
import retry.ResultHandler.{ noop, retryOnAllErrors }
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scodec.bits.ByteVector

import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.codeforces.models.*
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettingsConfigurable
import com.wenjunhuang.codeepiphany.model.{ ApiError, CodeDojo, SubmissionResult }
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

  def submitAnswer(
    contestId: Long,
    index: String,
    problemsetName: Option[String],
    programTypeId: String,
    code: String
  ): Stream[F, CodeForcesSubmissionResponse]
}

object CodeForcesApi {

  def apply[F[_]: { Async, Concurrent, HttpClientManager }]: CodeForcesApi[F] = new CodeForcesApi[F]
    with Http4sClientDsl[F] {

    private def useClient[A](fun: Client[F] => F[A]): F[A] = HttpClientManager[F].getClient.use(fun)

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
                .map(problem => problem.copy(contestId = Some(99999))) // fix contestId for acmsguru
                .zip(response.result.problemStatistics)
            }
        ).parMapN(_ ++ _)
      }

    override def checkLogin(): F[Boolean] = {
      HttpClientManager[F].getClient.use { client =>
        client
          .get(uri"https://codeforces.com/settings/general") { response =>
            response.body.compile.to(ByteVector).flatMap { content =>
              val html = content.decodeUtf8.getOrElse("")
              if Jsoup.parse(html).select("div.userbox").isEmpty then Async[F].pure(false)
              else Async[F].pure(true)
            }
          }
          .handleErrorWith { case status: UnexpectedStatus =>
            Async[F].pure(false)
          }
      }
    }

    override def getProblemTags: F[List[String]] = HttpClientManager[F].getClient.use { client =>
      client.expect[String](uri"https://codeforces.com/problemset").flatMap { content =>
        Async[F].delay {
          Jsoup.parse(content).select("label._FilterByTagsFrame_addTagLabel option").asScala.toList.collect {
            case elem
                if elem.hasAttr("value") && StringUtil
                  .isNotEmpty(elem.attr("value")) && elem.attr("value") != "combine-tags-by-or" =>
              elem.attr("value")
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
          val jsoup = Jsoup.parse(content)
          jsoup.select("div#pageContent div.ttypography").asScala.toList.headOption.map { element =>
            val supportedLanguages = jsoup
              .select("select[name=programTypeId] option")
              .asScala
              .toList
              .collect {
                case option if StringUtil.isNotEmpty(option.attr("value")) =>
                  CodeForcesSettingsConfigurable.CODEFORCES_LANGUAGES_REVERSE.get(option.attr("value"))
              }
              .collect { case Some(value) => value }
            CodeForcesChallengeData(
              contestId = contestId,
              index = index,
              description = element.outerHtml(),
              supportedLanguages.toSet
            )
          }
        }
      }

    private def prepareSubmitAnswer(): F[(String, String, String)] = {
      HttpClientManager[F].getClient.use { client =>
        client.expect[String](Method.GET(uri"https://codeforces.com/problemset/submit")).map { content =>
          val doc       = Jsoup.parse(content)
          val csrfToken = doc.select("meta[name=X-Csrf-Token]").attr("content")
          var ftaa      = ""
          var bfaa      = ""
          doc
            .select("script")
            .not("[src]")
            .asScala
            .toList
            .foreach { element =>
              val value = element.html()
              FTAA_REGEX
                .findFirstMatchIn(value)
                .map { m =>
                  ftaa = m.group(m.groupCount)
                }
                .orElse(BFAA_REGEX.findFirstMatchIn(value).map { m =>
                  bfaa = m.group(m.groupCount)
                })
            }
          (csrfToken, ftaa, bfaa)
        }
      }
    }

    private def extractSubmissionResult(content: String): F[CodeForcesSubmissionResponse] = {
      Async[F].delay {
        val parsed = Jsoup.parse(content)
        parsed
          .select("div.shiftUp > span.error")
          .not("[style]")
          .asScala
          .headOption match
          case Some(element) =>
            throw ApiError.BadRequest(CodeDojo.CodeForces, StringUtil.trim(element.text()))
          case _ =>
            parsed.select("div.datatable table tr[data-submission-id]").asScala.toList.headOption match {
              case Some(element) =>
                val tdList = element
                  .select("td")
                  .asScala
                  .toList
                tdList match
                  case num :: when :: who :: problem :: lang :: verdict :: time :: memory :: Nil =>
                    val (problemContestIdIndex, problemName) = StringUtil.trim(problem.text()).split("-", 2) match
                      case Array(id, name) => (StringUtil.trim(id), StringUtil.trim(name))
                      case Array(name)     => ("", StringUtil.trim(name))

                    CodeForcesSubmissionResponse(
                      submissionId = StringUtil.trim(num.text()).toLong,
                      when = parseDateTime(when.text()),
                      who = StringUtil.trim(who.text()),
                      problemContestIdIndex = problemContestIdIndex,
                      problemName = problemName,
                      lang = lang.text(),
                      verdict = verdict.text(),
                      time = time.text(),
                      memory = memory.text(),
                      result = SubmissionResult.Processing,
                      message = ""
                    )
                  case _ =>
                    throw ApiError.InvalidContent(CodeDojo.CodeForces, "Failed to extract submission result table")
              case None => throw ApiError.InvalidContent(CodeDojo.CodeForces, "Failed to extract submission result")
            }
      }
    }

    private def parseDateTime(dateTime: String): LocalDateTime =
      LocalDateTime
        .parse(dateTime, DateTimeFormatter.ofPattern("MMM/dd/yyyy HH:mm", Locale.ENGLISH))
        .atZone(ZoneId.of("UTC+3"))
        .toLocalDateTime

    private def getSubmitAnswerResult(
      oldResponse: CodeForcesSubmissionResponse,
      csrfToken: String
    ): F[CodeForcesSubmissionResponse] = {
      HttpClientManager[F].getClient.use { client =>
        client
          .expect[String](
            Method
              .POST(
                uri"https://codeforces.com/data/submitSource"
                  .withQueryParam("rv", scala.util.Random.nextLong().toHexString.substring(0, 9)),
                headers = Headers(
                  Referer(uri"https://codeforces.com/contest/2115/my"),
                  "x-csrf-token" -> csrfToken
                )
              )
              .withEntity(UrlForm("submissionId" -> oldResponse.submissionId.toString, "csrf_token" -> csrfToken))
          )
          .flatMap { content =>
            parse(content)
              .leftMap(e => ApiError.InvalidContent(CodeDojo.CodeForces, e.getMessage))
              .map { json =>
                JsonPath.root.verdict.string.getOption(json).map(CIString(_)) match {
                  case Some(verdict) if verdict.contains(CIString("Accepted")) =>
                    oldResponse.copy(result = SubmissionResult.Success)
                  case Some(verdict) if verdict.contains(CIString("Running")) =>
                    oldResponse.copy(result = SubmissionResult.Processing)
                  case Some(verdict) =>
                    val msg = JsonPath.root
                      .selectDynamic("checkerStdoutAndStderr#1")
                      .string
                      .getOption(json)
                      .filter(StringUtil.isNotEmpty)
                    verdict match {
                      case v if v.contains(CIString("Wrong answer")) =>
                        oldResponse.copy(
                          result = SubmissionResult.Failure,
                          message = msg.getOrElse(SubmissionResult.Failure.show)
                        )
                      case v if v.contains(CIString("Compilation error")) =>
                        oldResponse.copy(
                          result = SubmissionResult.CompilationError,
                          message = msg.getOrElse(SubmissionResult.CompilationError.show)
                        )
                      case v if v.contains(CIString("Time limit exceeded")) =>
                        oldResponse.copy(
                          result = SubmissionResult.Timeout,
                          message = msg.getOrElse(SubmissionResult.Timeout.show)
                        )
                      case v if v.contains(CIString("Runtime error")) =>
                        oldResponse.copy(
                          result = SubmissionResult.RuntimeError,
                          message = msg.getOrElse(SubmissionResult.RuntimeError.show)
                        )
                      case _ =>
                        oldResponse
                    }
                  case None => oldResponse
                }
              }
              .liftTo[F]
          }
      }
    }

    override def submitAnswer(
      contestId: Long,
      index: String,
      problemsetName: Option[String],
      programTypeId: String,
      code: String
    ): Stream[F, CodeForcesSubmissionResponse] =
      Stream
        .eval(prepareSubmitAnswer())
        .evalMap { case (csrfToken, ftaa, bfaa) =>
          useClient { client =>
            client
              .expect[String](
                Method
                  .POST(problemsetName match {
                    case None        => uri"https://codeforces.com/problemset/submit"
                    case Some(value) => uri"https://codeforces.com/problemsets" / value / "submit"
                  })
                  .withEntity(
                    UrlForm(
                      "csrf_token"            -> csrfToken,
                      "ftaa"                  -> ftaa,
                      "bfaa"                  -> bfaa,
                      "action"                -> "submitSolutionFormSubmitted",
                      "contestId"             -> contestId.toString,
                      "submittedProblemIndex" -> index,
                      "programTypeId"         -> programTypeId,
                      "source"                -> code,
                      "tabSize"               -> "4",
                      "sourceFile"            -> "",
                      "_tta"                  -> "460"
                    )
                  )
              )
              .flatMap(extractSubmissionResult)
              .map((csrfToken, _))
          }
        }
        .flatMap { case (csrfToken, response) =>
          Stream
            .repeatEval(Temporal[F].sleep(2.second))
            .evalScan(response) { (lastResponse, _) =>
              retryingOnErrors(getSubmitAnswerResult(lastResponse, csrfToken))(
                RetryPolicies.exponentialBackoff(2.second),
                retryOnAllErrors(noop)
              )
            }
            .flatMap { response =>
              response.result match
                case SubmissionResult.Processing => Stream(Option(response).widen)
                case _                           => Stream(Option(response).widen, None)
            }
            .unNoneTerminate
        }
  }

  private val FTAA_REGEX = """window\._ftaa\s*=\s*"([^"]*)"""".r
  private val BFAA_REGEX = """window\._bfaa\s*=\s*"([^"]*)"""".r
}
