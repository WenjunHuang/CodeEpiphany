package com.wenjunhuang.codeepiphany.atcoder.services

import cats.effect.{ Concurrent, Temporal }
import cats.effect.kernel.Async
import cats.syntax.all.*
import fs2.Stream
import io.circe.JsonObject
import org.http4s.{ Method, UrlForm }
import org.http4s.client.{ Client, UnexpectedStatus }
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri
import org.jsoup.Jsoup
import org.typelevel.ci.CIString
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.atcoder.models.*
import com.wenjunhuang.codeepiphany.atcoder.settings.AtCoderSettingsConfigurable.ATCODER_LANGUAGES_REVERSE
import com.wenjunhuang.codeepiphany.model.{ ApiError, CodeDojo, SubmissionResult }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager

trait AtCoderApi[F[_]] {
  def checkLogin(): F[Boolean]
  def getUserInfo: F[AtCoderUserInfo]
  def getAllProblems: F[List[AtCoderProblem]]
  def getAllContests: F[List[AtCoderContest]]
  def getAllProblemDifficulty: F[Map[String, Int]]
  def getChallengeData(contestId: String, problemId: String): F[Option[AtCoderChallengeData]]
  def submitAnswer(
    contestId: String,
    problemId: String,
    languageId: String,
    code: String
  ): Stream[F, AtCoderSubmissionResponse]
}

object AtCoderApi {
  def apply[F[_]: { Async, Concurrent, HttpClientManager }]: AtCoderApi[F] = new AtCoderApi[F] with Http4sClientDsl[F] {
    override def checkLogin(): F[Boolean] = useClient { client =>
      client
        .expect[String](uri"https://atcoder.jp/settings")
        .map { content =>
          !Jsoup.parse(content).select("#main-container #user-nav-tabs").isEmpty
        }
        .handleErrorWith { case status: UnexpectedStatus =>
          Async[F].pure(false)
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
          .expect[String](uri"https://atcoder.jp/contests" / contestId / "tasks" / problemId)
          .map { html =>
            val doc = Jsoup.parse(html)
            doc
              .select("div#main-container div.row > div")
              .asScala
              .drop(1)
              .headOption
              .map(it => it.html())
              .map { description =>
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
      contestId: String,
      problemId: String,
      languageId: String,
      code: String
    ): Stream[F, AtCoderSubmissionResponse] =
      Stream
        .eval(useClient { client =>
          client.expect[String](uri"https://atcoder.jp/contests" / contestId / "tasks" / problemId).map { html =>
            Jsoup
              .parse(html)
              .select("input[name='csrf_token']")
              .asScala
              .collectFirst { case element if StringUtil.isNotEmpty(element.attr("value")) => element.attr("value") }
              .getOrElse(throw ApiError.NotFound(CodeDojo.AtCoder, "CSRF token not found"))
          }
        })
        .evalMap { csrfToken =>
          useClient { client =>
            client
              .expect[String](
                Method
                  .POST(uri"https://atcoder.jp/contests" / contestId / "submit")
                  .withEntity(
                    UrlForm(
                      "data.TaskScreenName" -> problemId,
                      "data.LanguageId"     -> languageId,
                      "sourceCode"          -> code,
                      "csrf_token"          -> csrfToken
                    )
                  )
              )
              .map { html =>
                val doc = Jsoup
                  .parse(html)
                val maybeError = doc
                  .select("div[role='alert']")
                  .asScala
                  .headOption
                  .map(elem => StringUtil.trim(elem.ownText()))
                maybeError match
                  case Some(error) =>
                    throw ApiError.BadRequest(CodeDojo.AtCoder, error)
                  case None =>
                    val submissionId =
                      doc
                        .select("td.submission-score[data-id]")
                        .asScala
                        .headOption
                        .map(_.attr("data-id"))
                        .getOrElse(throw ApiError.NotFound(CodeDojo.AtCoder, "Submission ID not found"))
                    (csrfToken, AtCoderSubmissionResponse(submissionId, contestId, 0, SubmissionResult.Processing, ""))
              }
          }
        }
        .flatMap { (csrfToken, response) =>
          Stream
            .repeatEval(Temporal[F].sleep(2.second))
            .evalScan(response) { (lastResponse, _) => getSubmitAnswerResult(lastResponse, csrfToken) }
            .flatMap { response =>
              response.result match
                case SubmissionResult.Processing => Stream(Option(response).widen)
                case _                           => Stream(Option(response).widen, None)
            }
            .unNoneTerminate
        }

    private def getSubmitAnswerResult(
      oldResponse: AtCoderSubmissionResponse,
      csrfToken: String
    ): F[AtCoderSubmissionResponse] = {
      HttpClientManager[F].getClient.use { client =>
        client
          .expect[String](
            uri"https://atcoder.jp/contests" / oldResponse.contestId / "submissions" / oldResponse.submissionId
          )
          .map { html =>
            val doc = Jsoup
              .parse(html)
            val result = doc
              .select("td#judge-status > span")
              .asScala
              .headOption
              .map(it =>
                judgeStatusToSubmissionResult(StringUtil.notNullize(it.attr("title")), StringUtil.notNullize(it.text()))
              )
              .getOrElse(throw ApiError.NotFound(CodeDojo.AtCoder, "Submission result not found"))
            val msg =
              if result == SubmissionResult.CompilationError then
                doc
                  .select("pre")
                  .asScala
                  .lastOption
                  .map(_.text())
                  .getOrElse("")
              else ""

            oldResponse.copy(result = result, message = msg)
          }
          .recoverWith {
            case e: UnexpectedStatus if e.status.code == 503 =>
              Temporal[F].sleep(2.second) >> getSubmitAnswerResult(oldResponse, csrfToken)
          }
      }
    }

    private def useClient[A](f: Client[F] => F[A]): F[A] = HttpClientManager[F].getClient.use(f)

    private def judgeStatusToSubmissionResult(title: String, text: String): SubmissionResult = {
      if CIString(title) == CIString("Judging") then SubmissionResult.Processing
      else
        val ciText = CIString(text)
        if ciText.contains(CIString("AC")) then SubmissionResult.Success
        else if ciText.contains(CIString("WA")) then SubmissionResult.Failure
        else if ciText.contains(CIString("TLE")) then SubmissionResult.Timeout
        else if ciText.contains(CIString("MLE")) then SubmissionResult.MemoryLimitExceeded
        else if ciText.contains(CIString("RE")) then SubmissionResult.RuntimeError
        else if ciText.contains(CIString("CE")) then SubmissionResult.CompilationError
        else if ciText.contains(CIString("QLE")) then SubmissionResult.Failure
        else if ciText.contains(CIString("OLE")) then SubmissionResult.OutputLimitExceeded
        else if ciText.contains(CIString("IE")) then SubmissionResult.InternalError
        else if ciText.contains(CIString("WJ")) then SubmissionResult.Processing
        else if ciText.contains(CIString("WR")) then SubmissionResult.Processing
        else SubmissionResult.Failure
    }
  }
}
