package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.{ Concurrent, IO, Temporal }
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.Stream
import io.circe.*
import io.circe.optics.JsonPath
import io.circe.parser.parse
import io.circe.syntax.*
import org.http4s.{ Headers, Method, Request, Uri }
import org.http4s.circe.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri
import org.jsoup.Jsoup
import scala.concurrent.duration.*

import com.wenjunhuang.codeepiphany.hackerrank.models.*
import com.wenjunhuang.codeepiphany.hackerrank.models.HackerRankContest.{ Master, ProjectEuler }
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import scala.jdk.CollectionConverters.*

import com.wenjunhuang.codeepiphany.settings.ChallengeSettings

trait HackerRankApi {
  def getInitialData: IO[(HackerRankUserInfo, List[HackerRankChallengeDomain])]
  def getChallengeContent(challengeSlug: String, contest: HackerRankContest): IO[HackerRankChallengeContent]
  def getChallengeDetail(challengeSlug: String, contest: HackerRankContest): IO[HackerRankChallengeDetail]
  def searchChallenges(
    offset: Int,
    limit: Int,
    contest: HackerRankContest,
    domainSlug: String,
    status: List[ChallengeStatus] = Nil,
    skills: List[HackerRankChallengeSkill] = Nil,
    difficulties: List[ChallengeDifficulty] = Nil,
    subdomains: List[HackerRankChallengeSubdomain] = Nil
  ): IO[(Int, List[HackerRankChallengeDetail])]

  def searchChallengesWithKeyword(
    contest: HackerRankContest,
    keyword: String
  ): IO[List[(HackerRankContest, HackerRankChallengeSearchByKeyWord)]]

  def checkLogin(): IO[Boolean]

  def submitAnswer(
    challengeSlug: String,
    contest: HackerRankContest,
    language: Language,
    langVer: LanguageVersion,
    code: String
  ): Stream[IO, HackerRankSubmissionResponse]

  def runAnswer(
    challengeSlug: String,
    contest: HackerRankContest,
    language: Language,
    langVer: LanguageVersion,
    code: String
  ): Stream[IO, HackerRankRunCodeResponse]
}

object HackerRankApi extends HackerRankApi with Http4sClientDsl[IO] {

  override def checkLogin(): IO[Boolean] = HttpClientManager.getClient.use { client =>
    client
      .run(Request[IO](Method.GET, uri"https://www.hackerrank.com/community/v1/promotion_slots/banner-dashboard"))
      .use { response =>
        response.status match {
          case status if status.isSuccess => IO.pure(true)
          case _                          => IO.pure(false)
        }
      }
  }

  private def makeGetChallengeContentRequestUrl(problemSlug: String, contest: HackerRankContest): Uri = {
    val baseUri = uri"https://www.hackerrank.com"
    contest match {
      case HackerRankContest.Master => baseUri / "challenges" / problemSlug / "problem"
      case HackerRankContest.ProjectEuler =>
        baseUri / "contests" / "projecteuler" / "challenges" / problemSlug / "problem"
    }
  }

  override def getChallengeDetail(problemSlug: String, contest: HackerRankContest): IO[HackerRankChallengeDetail] =
    HttpClientManager.getClient.use { client =>
      client
        .expect[String](Request[IO](Method.GET, makeGetChallengeContentRequestUrl(problemSlug, contest)))
        .map { content =>
          Option(Jsoup.parse(content).selectFirst("script[id=initialData]")) match
            case None =>
              throw ApiError.InvalidContent(HackerRank, "can not find script[id=initialData] element in html")
            case Some(element) =>
              parse(Uri.decode(element.html())) match {
                case Left(e) =>
                  throw ApiError.InvalidContent(HackerRank, e.getMessage)
                case Right(json) =>
                  JsonPath.root.community.challenges.challenge
                    .selectDynamic(s"${contest.slug}/$problemSlug")
                    .detail
                    .json
                    .getOption(json)
                    .toRight(ApiError.InvalidContent(HackerRank, "invalid challenge content json"))
                    .flatMap { json =>
                      json.as[HackerRankChallengeDetail].leftMap { e =>
                        ApiError.InvalidContent(HackerRank, e.getMessage)
                      }
                    }
                    .fold(throw _, identity)
              }
        }
    }

  override def getChallengeContent(
    problemSlug: String,
    contest: HackerRankContest // hackerrank contest such as 'projecteuler'
  ): IO[HackerRankChallengeContent] =
    HttpClientManager.getClient.use { client =>
      client
        .expect[String](Request[IO](Method.GET, makeGetChallengeContentRequestUrl(problemSlug, contest)))
        .flatMap { content =>
          Option(Jsoup.parse(content).selectFirst("script[id=initialData]")) match
            case None =>
              IO.raiseError(ApiError.InvalidContent(HackerRank, "can not find element script[id=initialData]"))
            case Some(element) =>
              parse(Uri.decode(element.html())) match {
                case Left(e) => IO.raiseError(ApiError.InvalidContent(HackerRank, e.getMessage))
                case Right(json) =>
                  JsonPath.root.community.challenges.challenge
                    .selectDynamic(s"${contest.slug}/$problemSlug")
                    .detail
                    .json
                    .getOption(json)
                    .toRight(ApiError.InvalidContent(HackerRank, "invalid challenge content json"))
                    .flatMap { json =>
                      json
                        .as[HackerRankChallengeContent]
                        .map { content =>
                          val document = Jsoup.parse(content.detail.bodyHtml.getOrElse(""))
                          val testCases = document
                            .select(".challenge_sample_input pre")
                            .asScala
                            .zip(document.select(".challenge_sample_output pre").asScala)
                            .map { case (input, output) =>
                              ChallengeSettings.TestCase(input.text(), output.text())
                            }
                            .toList

                          content.copy(testCases = if (testCases.isEmpty) {
                            document.select(".challenge-body-html pre").asScala.toList.grouped(2).collect {
                              case Seq(input, output) =>
                                ChallengeSettings.TestCase(input.text(), output.text())
                            }.toList
                          } else testCases)
                        }
                        .leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
                    }
                    .liftTo[IO]
              }
        }
    }

  private def makeSearchChallengesUri(contest: HackerRankContest, domainSlug: String): Uri =
    val base = uri"https://www.hackerrank.com/rest/contests" / contest.slug
    if contest == HackerRankContest.ProjectEuler then base / "challenges"
    else base / "tracks" / domainSlug / "challenges"

  override def searchChallenges(
    offset: Int,
    limit: Int,
    contest: HackerRankContest,
    domainSlug: String,
    status: List[ChallengeStatus],
    skills: List[HackerRankChallengeSkill],
    difficulties: List[ChallengeDifficulty],
    subdomains: List[HackerRankChallengeSubdomain]
  ): IO[(Int, List[HackerRankChallengeDetail])] =
    HttpClientManager.getClient.use { client =>
      val request = Method.GET(
        makeSearchChallengesUri(contest, domainSlug)
          .withQueryParam("offset", offset)
          .withQueryParam("limit", limit)
          .withMultiValueQueryParams(
            Map(
              "filters[subdomains][]" -> subdomains.map(_.slug),
              "filters[status][]"     -> status.map(_.value),
              "filters[difficulty][]" -> difficulties.map(_.value),
              "filters[skills][]"     -> skills.map(_.value)
            ).filter(_._2.nonEmpty)
          )
      )
      client
        .expect[String](request)
        .flatMap { response =>
          parse(response)
            .leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
            .flatMap { result =>
              (
                JsonPath.root.total.int
                  .getOption(result)
                  .toRight(ApiError.InvalidContent(HackerRank, "invalid challenges list json")),
                JsonPath.root.models.json
                  .getOption(result)
                  .toRight(ApiError.InvalidContent(HackerRank, "invalid challenges list json"))
              ).mapN((_, _))
            }
            .flatMap { (total, items) =>
              items.as[List[HackerRankChallengeDetail]].map((total, _))
            }
            .liftTo[IO]
        }
    }

  override def searchChallengesWithKeyword(
    contest: HackerRankContest,
    keyword: String
  ): IO[List[(HackerRankContest, HackerRankChallengeSearchByKeyWord)]] = HttpClientManager.getClient.use { client =>
    client
      .expect[String](
        Method.GET(uri =
          uri"https://www.hackerrank.com/appsearch"
            .withQueryParam("contest_slug", contest.slug)
            .withQueryParam("query", keyword)
        )
      )
      .flatMap { response =>
        parse(response)
          .leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
          .flatMap { result =>
            JsonPath.root.challenges.json
              .getOption(result)
              .toRight(ApiError.InvalidContent(HackerRank, "invalid challenges list json"))
          }
          .flatMap { items =>
            items.as[List[HackerRankChallengeSearchByKeyWord]]
          }
          .map(_.map((contest, _)))
          .liftTo[IO]
      }
  }

  override def getInitialData: IO[(HackerRankUserInfo, List[HackerRankChallengeDomain])] =
    HttpClientManager.getClient.use { client =>
      val request = Method.GET(uri = uri"https://www.hackerrank.com/dashboard")
      client
        .expect[String](request)
        .flatMap { response =>
          val doc = Jsoup.parse(response)
          (Option(doc.selectFirst("script[id=initialUserData]")), Option(doc.selectFirst("script[id=initialData]")))
            .mapN(_ -> _)
            .toRight(
              ApiError
                .InvalidContent(HackerRank, "The dashboard HTML does not include initialUserData or initialData.")
            )
            .flatMap { case (initialUserData, initialData) =>
              (
                parse(Uri.decode(initialUserData.html)).leftMap(e =>
                  ApiError
                    .InvalidContent(HackerRank, s"The initialUserData script is not valid JSON for ${e.getMessage}")
                ),
                parse(Uri.decode(initialData.html)).leftMap(e =>
                  ApiError.InvalidContent(HackerRank, s"The initialData script is not valid JSON for ${e.getMessage}")
                )
              ).flatMapN { case (userData, data) =>
                (
                  userData.as[HackerRankUserInfo],
                  (
                    JsonPath.root.community.domains.list.arr.getOption(data),
                    JsonPath.root.community.domains.dict.as[Map[String, Json]].getOption(data)
                  ).mapN((_, _)).toRight(ApiError.InvalidContent(HackerRank, "invalid challenges list json"))
                ).mapN { case (userInfo, (domains, dict)) => (userInfo, domains, dict) }
              }
            }
            .map { case (userInfo, domains, dict) =>
              (
                userInfo,
                domains.mapFilter { domain =>
                  for {
                    id   <- JsonPath.root.id.int.getOption(domain)
                    name <- JsonPath.root.name.string.getOption(domain)
                    slug <- JsonPath.root.slug.string.getOption(domain)
                  } yield HackerRankChallengeDomain(
                    id,
                    name,
                    slug,
                    HackerRankContest.Master,
                    dict
                      .get(slug)
                      .map { d =>
                        JsonPath.root.chapters.arr
                          .getOption(d)
                          .getOrElse(Vector.empty)
                          .mapFilter(d => d.as[HackerRankChallengeSubdomain].toOption)
                          .toList
                      }
                      .getOrElse(Nil)
                  )
                }.toList
              )
            }
            .liftTo[IO]
        }
    }

  override def runAnswer(
    challengeSlug: String,
    contest: HackerRankContest,
    language: Language,
    langVer: LanguageVersion,
    code: String
  ): Stream[IO, HackerRankRunCodeResponse] = {
    Stream
      .eval(HttpClientManager.getClient.use { client =>
        getChallengeCSRFToken(client, contest, challengeSlug).flatMap { csrfToken =>
          val request = Method
            .POST(
              uri"https://www.hackerrank.com/rest/contests" / contest.slug / "challenges" / challengeSlug / "compile_tests",
              Headers("x-csrf-token" -> csrfToken)
            )
            .withEntity(
              JsonObject.fromMap(
                Map(
                  "language"       -> s"${language.value}${langVer.version}".asJson,
                  "playlist_slug"  -> "".asJson,
                  "customtestcase" -> false.asJson,
                  "code"           -> code.asJson
                )
              )
            )
          client.expect[String](request).flatMap { response =>
            parse(response)
              .leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
              .flatMap { json =>
                JsonPath.root.model.id.int
                  .getOption(json)
                  .toRight(ApiError.InvalidContent(HackerRank, s"invalid run code response json with ${json.noSpaces}"))
              }
              .liftTo[IO]
          }
        }
      })
      .flatMap { runId =>
        // keep running getRunCodeResult until RunCodeResponse.status is 1 (still pass to next step) and pass RunCodeResponse to the next step
        Stream
          .repeatEval(
            Temporal[IO].sleep(1.second) *>
              getRunCodeResult(contest, challengeSlug, runId)
          )
          .flatMap { result =>
            if result.status == 1 then Stream(Option(result), None)
            else Stream(Option(result))
          }
          .unNoneTerminate
      }

  }

  private def getChallengeCSRFToken(
    client: Client[IO],
    contest: HackerRankContest,
    challengeSlug: String
  ): IO[String] = {
    val request = Method.GET(contest match
      case Master => uri"https://www.hackerrank.com/challenges" / challengeSlug / "problem"
      case ProjectEuler =>
        uri"https://www.hackerrank.com/contests/projecteuler/challenges" / challengeSlug / "problem")
    client.expect[String](request).flatMap { response =>
      val doc = Jsoup.parse(response)
      Option(doc.selectFirst("meta[id=csrf-token]"))
        .map(_.attr("content"))
        .toRight(ApiError.InvalidContent(HackerRank, "cannot find csrf token"))
        .liftTo[IO]
    }
  }

  private def getRunCodeResult(
    contest: HackerRankContest,
    challengeSlug: String,
    runId: Int
  ): IO[HackerRankRunCodeResponse] =
    HttpClientManager.getClient.use { client =>
      val request = Method.GET(
        uri"https://www.hackerrank.com/rest/contests" / contest.slug / "challenges" / challengeSlug / "compile_tests" / runId.toString
      )
      client.expect[String](request).flatMap { response =>
        parse(response)
          .leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
          .flatMap { json =>
            JsonPath.root.model.json
              .getOption(json)
              .toRight(ApiError.InvalidContent(HackerRank, s"invalid run code response json with ${json.noSpaces}"))
              .flatMap(_.as[HackerRankRunCodeResponse].leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage)))
          }
          .liftTo[IO]
      }
    }

  private def getSubmitCodeResult(
    contest: HackerRankContest,
    challengeSlug: String,
    submissionId: Long
  ): IO[HackerRankSubmissionResponse] =
    HttpClientManager.getClient.use { client =>
      val request = Method.GET(
        uri"https://www.hackerrank.com/rest/contests" / contest.slug / "challenges" / challengeSlug / "submissions" / submissionId.toString
      )
      client.expect[String](request).flatMap { response =>
        parse(response)
          .leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
          .flatMap { json =>
            JsonPath.root.model.json
              .getOption(json)
              .toRight(ApiError.InvalidContent(HackerRank, s"invalid submit code response json with ${json.noSpaces}"))
              .flatMap(
                _.as[HackerRankSubmissionResponse].leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
              )
          }
          .liftTo[IO]
      }
    }

  override def submitAnswer(
    challengeSlug: String,
    contest: HackerRankContest,
    language: Language,
    langVer: LanguageVersion,
    code: String
  ): Stream[IO, HackerRankSubmissionResponse] = Stream
    .eval(HttpClientManager.getClient.use { client =>
      getChallengeCSRFToken(client, contest, challengeSlug).flatMap { csrfToken =>
        val request = Method
          .POST(
            uri"https://www.hackerrank.com/rest/contests" / contest.slug / "challenges" / challengeSlug / "submissions",
            Headers("x-csrf-token" -> csrfToken)
          )
          .withEntity(
            Map(
              "language"       -> s"${language.value}${langVer.version}",
              "contest_slug"   -> contest.slug,
              "challenge_slug" -> challengeSlug,
              "code"           -> code
            ).asJson
          )
        client.expect[String](request).flatMap { response =>
          parse(response)
            .leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
            .flatMap { json =>
              JsonPath.root.model.id.long
                .getOption(json)
                .toRight(
                  ApiError.InvalidContent(HackerRank, s"invalid submit code response json with ${json.noSpaces}")
                )
            }
            .liftTo[IO]
        }
      }
    })
    .flatMap { submissionId =>
      // keep running getSubmitCodeResult until SubmissionResponse.status is not "Processing"  and pass it to the next step
      Stream
        .repeatEval(
          Temporal[IO].sleep(3.second) *>
            getSubmitCodeResult(contest, challengeSlug, submissionId)
        )
        .flatMap { result =>
          if result.scoreProcessed == 3 then Stream(Option(result), None)
          else Stream.emit(Option(result))
        }
        .unNoneTerminate
    }
}
