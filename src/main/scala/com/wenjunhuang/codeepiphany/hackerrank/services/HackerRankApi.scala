package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.{Async, Concurrent}
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.model.{ApiError, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.http.HttpClientKeeper
import io.circe.{Decoder, Json}
import io.circe.optics.JsonPath
import io.circe.parser.parse
import io.circe.syntax.*
import org.http4s.{EntityDecoder, Method, Request, Uri}
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri
import org.jsoup.Jsoup

trait HackerRankApi[F[_]] {
  def getInitialData: F[(UserInfo, List[ChallengeDomain])]
  def getChallengeContent(challengeSlug: String, contest: Contest): F[Option[ChallengeContent]]
  def getChallengeDetail(challengeSlug: String, contest: Contest): F[Option[ChallengeDetail]]
  def searchChallenges(
    offset: Int,
    limit: Int,
    contest: Contest,
    domainSlug: String,
    status: List[ChallengeStatus] = Nil,
    skills: List[ChallengeSkill] = Nil,
    difficulties: List[ChallengeDifficulty] = Nil,
    subdomains: List[ChallengeSubdomain] = Nil
  ): F[(Int, List[ChallengeDetail])]

  def searchChallengesWithKeyword(contest: Contest, keyword: String): F[List[(Contest, ChallengeSearchByKeyWord)]]

  def checkLogin(): F[Boolean]

  def submitAnswer(
    challengeSlug: String,
    contest: Contest,
    language: Language,
    langVer: LanguageVersion,
    code: String
  ): F[Unit]
}

object HackerRankApi {
  def apply[F[_]: Async: Concurrent: HttpClientKeeper](): HackerRankApi[F] = new HackerRankApi[F]
    with Http4sClientDsl[F] {

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

    private def makeGetChallengeContentRequestUrl(problemSlug: String, contest: Contest): Uri = {
      val baseUri = uri"https://www.hackerrank.com"
      contest match {
        case Contest.Master       => baseUri / "challenges" / problemSlug / "problem"
        case Contest.ProjectEuler => baseUri / "contests" / "projecteuler" / "challenges" / problemSlug / "problem"
      }
    }

    override def getChallengeDetail(problemSlug: String, contest: Contest): F[Option[ChallengeDetail]] =
      HttpClientKeeper[F].getClient.use { client =>
        client
          .expect[String](Request[F](Method.GET, makeGetChallengeContentRequestUrl(problemSlug, contest)))
          .map { content =>
            val doc = Jsoup.parse(content)
            (
              Option(doc.selectFirst("div[class=challenge-body-html]")),
              Option(doc.selectFirst("script[id=initialData]"))
            ).mapN { case (questionBody, questionCode) =>
              parse(Uri.decode(questionCode.html())) match {
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
                      json.as[ChallengeDetail]
                        .leftMap{ e =>
                          ApiError.InvalidContent(HackerRank, e.getMessage)
                        }
                    }
                    .fold(throw _, identity)
              }
            }
          }
      }

    override def getChallengeContent(
      problemSlug: String,
      contest: Contest // hackerrank contest such as 'projecteuler'
    ): F[Option[ChallengeContent]] =
      HttpClientKeeper[F].getClient.use { client =>
        client
          .expect[String](Request[F](Method.GET, makeGetChallengeContentRequestUrl(problemSlug, contest)))
          .map { content =>
            val doc = Jsoup.parse(content)
            (
              Option(doc.selectFirst("div[class=challenge-body-html]")),
              Option(doc.selectFirst("script[id=initialData]"))
            ).mapN { case (questionBody, questionCode) =>
              parse(Uri.decode(questionCode.html())) match {
                case Left(e) => throw ApiError.InvalidContent(HackerRank, e.getMessage)
                case Right(json) =>
                  JsonPath.root.community.challenges.challenge
                    .selectDynamic(s"${contest.slug}/$problemSlug")
                    .detail
                    .json
                    .getOption(json)
                    .toRight(ApiError.InvalidContent(HackerRank, "invalid challenge content json"))
                    .flatMap { json =>
                      json.as[ChallengeContent].leftMap(e => ApiError.InvalidContent(HackerRank, e.getMessage))
                    }
                    .fold(throw _, identity)
              }
            }
          }
      }

    private def makeSearchChallengesUri(contest: Contest, domainSlug: String): Uri =
      val base = uri"https://www.hackerrank.com/rest/contests" / contest.slug
      if contest == Contest.ProjectEuler then base / "challenges"
      else base / "tracks" / domainSlug / "challenges"

    override def searchChallenges(
      offset: Int,
      limit: Int,
      contest: Contest,
      domainSlug: String,
      status: List[ChallengeStatus],
      skills: List[ChallengeSkill],
      difficulties: List[ChallengeDifficulty],
      subdomains: List[ChallengeSubdomain]
    ): F[(Int, List[ChallengeDetail])] =
      HttpClientKeeper[F].getClient.use { client =>
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
                items.as[List[ChallengeDetail]].map((total, _))
              }
              .liftTo[F]
          }
      }

    override def searchChallengesWithKeyword(
      contest: Contest,
      keyword: String
    ): F[List[(Contest, ChallengeSearchByKeyWord)]] = HttpClientKeeper[F].getClient.use { client =>
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
              items.as[List[ChallengeSearchByKeyWord]]
            }
            .map(_.map((contest, _)))
            .liftTo[F]
        }
    }

    override def getInitialData: F[(UserInfo, List[ChallengeDomain])] = HttpClientKeeper[F].getClient.use { client =>
      val request = Method.GET(uri = uri"https://www.hackerrank.com/dashboard")
      client
        .expect[String](request)
        .flatMap { response =>
          val doc = Jsoup.parse(response)
          (Option(doc.selectFirst("script[id=initialUserData]")), Option(doc.selectFirst("script[id=initialData]")))
            .mapN(_ -> _)
            .toRight(
              ApiError.InvalidContent(HackerRank, "The dashboard HTML does not include initialUserData or initialData.")
            )
            .flatMap { case (initialUserData, initialData) =>
              (
                parse(Uri.decode(initialUserData.html)).leftMap(e =>
                  ApiError.InvalidContent(HackerRank, "The initialUserData script is not valid JSON")
                ),
                parse(Uri.decode(initialData.html)).leftMap(e =>
                  ApiError.InvalidContent(HackerRank, "The initialData script is not valid JSON")
                )
              ).flatMapN { case (userData, data) =>
                (
                  userData.as[UserInfo],
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
                  } yield ChallengeDomain(
                    id,
                    name,
                    slug,
                    Contest.Master,
                    dict
                      .get(slug)
                      .map { d =>
                        JsonPath.root.chapters.arr
                          .getOption(d)
                          .getOrElse(Vector.empty)
                          .mapFilter(d => d.as[ChallengeSubdomain].toOption)
                          .toList
                      }
                      .getOrElse(Nil)
                  )
                }.toList
              )
            }
            .liftTo[F]
        }
    }

    override def submitAnswer(
      challengeSlug: String,
      contest: Contest,
      language: Language,
      langVer: LanguageVersion,
      code: String
    ): F[Unit] = HttpClientKeeper[F].getClient.use { client =>
      val request = Method
        .POST(uri"https://www.hackerrank.com/rest/contests" / contest.slug / "challenges "/ challengeSlug / "submissions")
        .withEntity(
          Map(
            "language"       -> s"${language.value}${langVer.version}",
            "contest_slug"   -> contest.slug,
            "challenge_slug" -> challengeSlug,
            "code"           -> code
          ).asJson
        )
      client.expect[String](request).void
    }
  }
}
