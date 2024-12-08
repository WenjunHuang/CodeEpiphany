package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.Concurrent
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.controllers.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.model.{ ApiError, Language }
import io.circe.Json
import io.circe.optics.JsonPath
import io.circe.parser.parse
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri
import org.http4s.{ EntityDecoder, Headers, Method, Request, Uri }
import org.jsoup.Jsoup

trait HackerRankApi[F[_]] {
  def getChallengeDomains: F[List[ChallengeDomain]]
  def getChallengeContent(problemSlug: String, contest: Option[String], language: Language): F[Option[QuestionContent]]
  def searchChallenges(
      offset: Int,
      limit: Int,
      contest: Option[String],
      topicSlug: Option[String],
      status: List[ChallengeStatus] = Nil,
      skills: List[ChallengeSkill] = Nil,
      difficulties: List[ChallengeDifficulty] = Nil,
      subdomains: List[ChallengeSubdomain] = Nil
  ): F[List[ChallengeListItem]]

  def checkLogin(): F[Boolean]
}

object HackerRankApi {
  def apply[F[_]: Concurrent: HttpClientKeeper](): HackerRankApi[F] = new HackerRankApi[F] with Http4sClientDsl[F] {
    override def checkLogin(): F[Boolean] = HttpClientKeeper[F].getClient.use { client =>
      client
        .run(Request[F](Method.GET, uri"https://www.hackerrank.com/community/v1/promotion_slots/banner-dashboard"))
        .use { response =>
          response.status match {
            case status if status.isSuccess => true.pure[F]
            case _                          => false.pure[F]
          }
        }
    }

    private def makeRequestUrl(problemSlug: String, contest: Option[String]): Uri = {
      val baseUri = uri"https://www.hackerrank.com"
      contest match {
        case Some(contest) => baseUri / "contests" / contest / "challenges" / problemSlug / "problem"
        case None          => baseUri / "challenges" / problemSlug / "problem"
      }
    }

    override def getChallengeContent(
        problemSlug: String,
        contest: Option[String], // hackerrank contest such as 'projecteuler'
        language: Language
    ): F[Option[QuestionContent]] =
      HttpClientKeeper[F].getClient.use { client =>
        client
          .expect[String](
            Request[F](
              Method.GET,
              makeRequestUrl(problemSlug, contest)
            )
          )
          .map { content =>
            val doc = Jsoup.parse(content)
            (Option(doc.selectFirst("div[class=challenge-body-html]")), Option(doc.selectFirst("script[id=initialData]"))).mapN { case (questionBody, questionCode) =>
              parse(Uri.decode(questionCode.html())) match {
                case Left(e) => throw ApiError.InvalidContent(HackerRank, e.getMessage)
                case Right(json) =>
                  val codeTemplate =
                    JsonPath.root.community.challenges.challenge
                      .selectDynamic(s"${contest.getOrElse("master")}/$problemSlug")
                      .detail
                      .selectDynamic(s"${language.value}_template")
                      .string
                      .getOption(json)
                  val codeTemplateHead =
                    JsonPath.root.community.challenges.challenge
                      .selectDynamic(s"${contest.getOrElse("master")}/$problemSlug")
                      .detail
                      .selectDynamic(s"${language.value}_template_head")
                      .string
                      .getOption(json)
                  val codeTemplateTail =
                    JsonPath.root.community.challenges.challenge
                      .selectDynamic(s"${contest.getOrElse("master")}/$problemSlug")
                      .detail
                      .selectDynamic(s"${language.value}_template_tail")
                      .string
                      .getOption(json)
                  QuestionContent(problemSlug, questionBody.html(), codeTemplateHead.getOrElse("") + codeTemplate.getOrElse("") + codeTemplateTail.getOrElse(""), language)
              }
            }
          }
      }

    private def makeSearchChallengesUri(contest: Option[String], topicSlug: Option[String]): Uri =
      val base = uri"https://www.hackerrank.com/rest/contests" / contest.getOrElse("master")
      topicSlug match
        case None       => base / "challenges"
        case Some(slug) => base / "tracks" / slug / "challenges"

    override def searchChallenges(
        offset: Int,
        limit: Int,
        contest: Option[String],
        topicSlug: Option[String],
        status: List[ChallengeStatus],
        skills: List[ChallengeSkill],
        difficulties: List[ChallengeDifficulty],
        subdomains: List[ChallengeSubdomain]
    ): F[List[ChallengeListItem]] =
      HttpClientKeeper[F].getClient.use { client =>
        val request = Method.GET(
          makeSearchChallengesUri(contest, topicSlug)
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
          .map { response =>
            parse(response)
              .leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
              .flatMap(JsonPath.root.models.arr.getOption(_).toRight(ApiError.InvalidContent(HackerRank, "invalid challenges list json"))) match
              case Left(e) => throw e
              case Right(json) =>
                json.mapFilter { j =>
                  j.as[ChallengeListItem].toOption
                }.toList
          }
      }

    override def getChallengeDomains: F[List[ChallengeDomain]] = HttpClientKeeper[F].getClient.use { client =>
      val request = Method.GET(uri = uri"https://www.hackerrank.com/dashboard")
      client.expect[String](request).map { response =>
        val doc = Jsoup.parse(response)
        Option(doc.selectFirst("script[id=initialData]")).map { element =>
          parse(Uri.decode(element.html())) match
            case Left(e) => throw ApiError.InvalidContent(HackerRank, e.getMessage)
            case Right(json) =>
              json.asObject
              (JsonPath.root.community.domains.list.arr.getOption(json), JsonPath.root.community.domains.dict.as[Map[String, Json]].getOption(json)).mapN { case (domains, dict) =>
              }
        }
        Nil
      }
    }
  }
}
