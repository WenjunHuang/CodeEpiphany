package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.{ Async, Concurrent, Temporal }
import cats.effect.implicits.*
import cats.syntax.all.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri
import org.http4s.{ FormDataDecoder, Headers, Method, Uri, UrlForm }
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters.*
import fs2.Stream
import io.circe.Json
import io.circe.optics.JsonPath
import io.circe.parser.parse
import java.time.{ LocalDateTime, ZoneId, ZonedDateTime }
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import java.util.Locale
import org.http4s.client.{ Client, UnexpectedStatus }
import org.http4s.headers.Referer
import org.typelevel.ci.CIString
import scala.util.matching.Regex
import scala.concurrent.duration.*

import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.codeforces.models.{
  CodeForcesChallengeData,
  CodeForcesProblem,
  CodeForcesProblemResponse,
  CodeForcesProblemStatistics,
  CodeForcesSubmissionResponse
}
import com.wenjunhuang.codeepiphany.leetcode.model.submitAnswer.LeetCodeSubmitAnswerResult
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

  def apply[F[_]: Async: Concurrent: HttpClientManager](): CodeForcesApi[F] = new CodeForcesApi[F]
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
          .toList
          .headOption match
          case Some(element) =>
            throw ApiError.InvalidContent(CodeDojo.CodeForces, element.text())
          case _ =>
            parsed.select("div.datatable table tr[data-submission-id]").asScala.toList.headOption match {
              case Some(element) =>
                val tdList = element
                  .select("td")
                  .asScala
                  .toList
                tdList match
                  case num :: when :: who :: problem :: lang :: verdict :: time :: memory :: Nil =>
                    val (problemContestIdIndex, problemName) = StringUtil.trim(problem.text()).split("-") match
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

    private def parseDateTime(dateTime: String): LocalDateTime = {
      val formatter     = DateTimeFormatter.ofPattern("MMM/dd/yyyy HH:mm", Locale.ENGLISH)
      val localDateTime = LocalDateTime.parse(dateTime, formatter)
      localDateTime.atZone(ZoneId.of("UTC+3")).toLocalDateTime
    }

    private def getSubmitAnswerResult(
      oldResponse: CodeForcesSubmissionResponse,
      csrfToken: String
    ): F[CodeForcesSubmissionResponse] = {
      HttpClientManager[F].getClient.use { client =>
        client
          .expect[String](
            Method
              .POST(
                uri"https://codeforces.com/data/submitSource",
                headers = Headers(Referer(uri"https://codeforces.com/problemset/status?my=on"))
              )
              .withEntity(UrlForm("submissionId" -> oldResponse.submissionId.toString, "csrf_token" -> csrfToken))
          )
          .flatMap { content =>
            parse(content)
              .leftMap(e => ApiError.InvalidContent(CodeDojo.CodeForces, e.getMessage))
              .map { json =>
                JsonPath.root.verdict.string.getOption(json) match
                  case Some(value) =>
                    val ciString = CIString(value)
                    if ciString.contains(CIString("Accepted")) then oldResponse.copy(result = SubmissionResult.Success)
                    else
                      val msg =
                        JsonPath.root.selectDynamic("checkerStdoutAndStderr#1").string.getOption(json).getOrElse("")
                      if ciString.contains(CIString("Wrong answer")) then
                        oldResponse.copy(result = SubmissionResult.Failure, message = msg)
                      else if ciString.contains(CIString("Compilation error")) then
                        oldResponse.copy(result = SubmissionResult.CompilationError, message = msg)
                      else if ciString.contains(CIString("Time limit exceeded")) then
                        oldResponse.copy(result = SubmissionResult.Timeout, message = msg)
                      else if ciString.contains(CIString("Runtime error")) then
                        oldResponse.copy(result = SubmissionResult.RuntimeError, message = msg)
                      else oldResponse
                  case _ =>
                    oldResponse
              }
              .liftTo[F]
          }
          .recoverWith {
            case e: UnexpectedStatus if e.status.code == 503 =>
              Temporal[F].sleep(2.second) >> getSubmitAnswerResult(oldResponse, csrfToken)
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
              .flatMap { content =>
                extractSubmissionResult(content)
              }
              .map { response =>
                (csrfToken, response)
              }
          }
        }
        .flatMap { case (csrfToken, response) =>
          Stream
            .repeatEval(Temporal[F].sleep(2.second))
            .evalScan(response) { (oldResponse, _) => getSubmitAnswerResult(oldResponse, csrfToken) }
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
