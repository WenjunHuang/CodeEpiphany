package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.{ Async, Concurrent, Resource }
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.leetcode.model.{
  LeetCodeChallengeList,
  LeetCodeFavoriteItem,
  LeetCodeGraphQLRequest,
  LeetCodeTagTypeWithTags,
  LeetCodeUserInfo
}
import com.wenjunhuang.codeepiphany.model.{ ApiError, CodeDojo }
import com.wenjunhuang.codeepiphany.services.http.HttpClientKeeper
import io.circe.Json
import io.circe.optics.JsonPath
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Referer
import org.http4s.{ Headers, Method, Uri }
import org.typelevel.ci.CIString

import scala.io.{ BufferedSource, Source }

trait LeetCodeApi[F[_]] {
  def getFavoriteList: F[List[LeetCodeFavoriteItem]]
  def getTagTypeWithTags: F[List[LeetCodeTagTypeWithTags]]
  def searchChallenges(offset: Int, limit: Int): F[LeetCodeChallengeList]
  def getUserInfo(): F[LeetCodeUserInfo]
  def checkLogin(): F[Boolean]
}

object LeetCodeApi {
  def apply[F[_]: Async: Concurrent: HttpClientKeeper](dojo: CodeDojo): LeetCodeApi[F] = new LeetCodeApi[F]
    with Http4sClientDsl[F] {
    private def commonHeaders(csrfToken: String) =
      Headers("x-csrftoken" -> csrfToken, Referer(Uri.unsafeFromString(s"https://${dojo.domain.toString}/")))
    override def getFavoriteList: F[List[LeetCodeFavoriteItem]] = HttpClientKeeper[F].getClient.use { client =>
      client.expect[String](s"https://${dojo.domain.toString}/problems/api/favorites/").flatMap { response =>
        decode[List[LeetCodeFavoriteItem]](response).liftTo[F]
      }
    }

    override def checkLogin(): F[Boolean] = HttpClientKeeper[F].getClient.use { client =>
      openGraphQLFile(dojo, "globalData").use { file =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[String](
              Method
                .POST(
                  Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/noj-go/"),
                  headers = commonHeaders(csrfToken)
                )
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "globalData",
                    query = file.mkString,
                    variables = Map.empty[String, String].asJsonObject
                  )
                )
            )
            .flatMap { response =>
              parse(response)
                .liftTo[F]
                .flatMap { json =>
                  JsonPath.root.data.userStatus.isSignedIn.boolean
                    .getOption(json)
                    .toRight(ApiError.InvalidContent(dojo, "can not find 'data.userStatus.isSignedIn' in json"))
                    .liftTo[F]
                }
            }
        }
      }
    }

    override def getUserInfo(): F[LeetCodeUserInfo] = HttpClientKeeper[F].getClient.use { client =>
      openGraphQLFile(dojo, "globalData").use { file =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[String](
              Method
                .POST(
                  Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/"),
                  headers = commonHeaders(csrfToken)
                )
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "globalData",
                    query = file.mkString,
                    variables = Map.empty[String, String].asJsonObject
                  )
                )
            )
            .flatMap { response =>
              parse(response)
                .liftTo[F]
                .flatMap { json =>
                  JsonPath.root.data.userStatus.json
                    .getOption(json)
                    .toRight(ApiError.InvalidContent(dojo, "can not find 'data.userStatus' in json"))
                    .flatMap(_.as[LeetCodeUserInfo].leftMap(e => ApiError.InvalidContent(dojo, e.message)))
                    .liftTo[F]
                }
            }
        }
      }
    }

    override def getTagTypeWithTags: F[List[LeetCodeTagTypeWithTags]] = HttpClientKeeper[F].getClient.use { client =>
      openGraphQLFile(dojo, "questionTagTypeWithTags").use { file =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[String](
              Method
                .POST(
                  Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/"),
                  headers = commonHeaders(csrfToken)
                )
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "questionTagTypeWithTags",
                    query = file.mkString,
                    variables = Map.empty[String, String].asJsonObject
                  )
                )
            )
            .flatMap { response =>
              parse(response)
                .liftTo[F]
                .flatMap { json =>
                  JsonPath.root.data.questionTagTypeWithTags.json
                    .getOption(json)
                    .toRight(ApiError.InvalidContent(dojo, "can not find 'questionTagTypeWithTags' in json"))
                    .flatMap(_.as[List[LeetCodeTagTypeWithTags]])
                    .liftTo[F]
                }
            }
        }
      }
    }

    override def searchChallenges(offset: Int, limit: Int): F[LeetCodeChallengeList] =
      HttpClientKeeper[F].getClient.use { client =>
        openGraphQLFile(dojo, "problemsetQuestionList").use { file =>
          getCSRFToken.flatMap { csrfToken =>
            client
              .expect[String](
                Method
                  .POST(
                    Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/"),
                    headers = commonHeaders(csrfToken)
                  )
                  .withEntity(
                    LeetCodeGraphQLRequest(
                      operationName = "problemsetQuestionList",
                      query = file.mkString,
                      variables = Map(
                        "skip"         -> offset.asJson,
                        "limit"        -> limit.asJson,
                        "categorySlug" -> "all-code-essentials".asJson,
                        "filters"      -> Map.empty[String, String].asJson
                      ).asJsonObject
                    )
                  )
              )
              .flatMap { response =>
                parse(response)
                  .liftTo[F]
                  .flatMap { json =>
                    JsonPath.root.data.problemsetQuestionList.json
                      .getOption(json)
                      .toRight(ApiError.InvalidContent(dojo, "can not find 'problemsetQuestionList' in json"))
                      .flatMap(_.as[LeetCodeChallengeList])
                      .liftTo[F]
                  }
              }
          }
        }
      }

    private def getCSRFToken: F[String] =
      HttpClientKeeper[F].findCookieForHost(CIString(dojo.domain.toString), CIString("csrftoken")).map {
        case Some(cookie) => cookie.getValue
        case None         => ""
      }

    def openGraphQLFile(dojo: CodeDojo, fileName: String): Resource[F, BufferedSource] = Resource
      .fromAutoCloseable[F, BufferedSource](
        Async[F]
          .blocking(
            Source.fromInputStream(
              if dojo == CodeDojo.LeetCodeCN then
                Option(getClass.getResourceAsStream(s"graphql/${fileName}_cn.graphql"))
                  .getOrElse(getClass.getResourceAsStream(s"graphql/${fileName}.graphql"))
              else getClass.getResourceAsStream(s"graphql/${fileName}.graphql")
            )
          )
      )
  }
}
