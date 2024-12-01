package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.Concurrent
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.controllers.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.hackerrank.model.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.model.{ ApiError, Language }
import io.circe.optics.JsonPath
import io.circe.parser.parse
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits.uri
import org.http4s.{ EntityDecoder, Headers, Method, Request, Uri }
import org.jsoup.Jsoup

trait HackerRankApi[F[_]] {
  def getQuestionContent(problemSlug: String, language: Language): F[Option[QuestionContent]]
  def getAlgorithmsChallenges(
      offset: Int,
      limit: Int,
      status: List[QuestionStatus] = Nil,
      skills: List[QuestionSkill] = Nil,
      difficulties: List[QuestionDifficulty] = Nil,
      subdomains: List[QuestionSubdomain] = Nil
  ): F[List[ChallengeListItem]]
}

object HackerRankApi {
  def apply[F[_]: Concurrent](httpClientKeeper: HttpClientKeeper[F]): HackerRankApi[F] = new HackerRankApi[F] with Http4sClientDsl[F] {
    override def getQuestionContent(problemSlug: String, language: Language): F[Option[QuestionContent]] =
      httpClientKeeper.getClient.use { client =>
        client
          .expect[String](
            Request[F](
              Method.GET,
              uri"https://www.hackerrank.com/challenges" / problemSlug / "problem",
              headers = Headers("User-Agent" -> "Mozilla/5.0")
            )
          )
          .map { content =>
            val doc = Jsoup.parse(content)
            (Option(doc.selectFirst("div[class=challenge-body-html]")), Option(doc.selectFirst("script[id=initialData]"))).mapN { case (questionBody, questionCode) =>
              parse(Uri.decode(questionCode.html())) match {
                case Left(e) => throw ApiError.InvalidContent(HackerRank, e.getMessage)
                case Right(json) =>
                  val codeTemplate =
                    JsonPath.root.community.challenges.challenge.selectDynamic(s"master/$problemSlug").detail.selectDynamic(s"${language.value}_template").string.getOption(json)
                  val codeTemplateHead =
                    JsonPath.root.community.challenges.challenge.selectDynamic(s"master/$problemSlug").detail.selectDynamic(s"${language.value}_template_head").string.getOption(json)
                  val codeTemplateTail =
                    JsonPath.root.community.challenges.challenge.selectDynamic(s"master/$problemSlug").detail.selectDynamic(s"${language.value}_template_tail").string.getOption(json)
                  QuestionContent(problemSlug, questionBody.html(), codeTemplateHead.getOrElse("") + codeTemplate.getOrElse("") + codeTemplateTail.getOrElse(""), language)
              }
            }
          }
      }

    override def getAlgorithmsChallenges(
        offset: Int,
        limit: Int,
        status: List[QuestionStatus],
        skills: List[QuestionSkill],
        difficulties: List[QuestionDifficulty],
        subdomains: List[QuestionSubdomain]
    ): F[List[ChallengeListItem]] =
      httpClientKeeper.getClient.use { client =>
        val request = Method.GET(
          uri"https://www.hackerrank.com/rest/contests/master/tracks/algorithms/challenges"
            .withQueryParam("offset", offset)
            .withQueryParam("limit", limit)
            .withMultiValueQueryParams(
              Map(
                "filters[subdomains][]" -> subdomains.map(_.value),
                "filters[status][]"     -> status.map(_.value),
                "filters[difficulty][]" -> difficulties.map(_.value),
                "filters[skills][]"     -> skills.map(_.value)
              ).filter(_._2.nonEmpty)
            ),
          headers = Headers("User-Agent" -> "Mozilla/5.0")
        )
        println(request.asCurl())
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
  }
}
