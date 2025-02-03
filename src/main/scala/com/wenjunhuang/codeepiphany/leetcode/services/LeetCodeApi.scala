package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.{ Async, Concurrent, Resource, Temporal }
import cats.syntax.all.*
import fs2.Stream
import io.circe.{ Json, JsonObject }
import io.circe.optics.JsonPath
import io.circe.syntax.*
import org.http4s.{ Headers, Method, Uri }
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.Client
import org.http4s.headers.Referer
import org.typelevel.ci.CIString
import scala.concurrent.duration.*
import scala.io.{ BufferedSource, Source }

import com.intellij.util.LineSeparator

import com.wenjunhuang.codeepiphany.leetcode.model.*
import com.wenjunhuang.codeepiphany.leetcode.model.runCode.*
import com.wenjunhuang.codeepiphany.leetcode.model.submitAnswer.{
  LeetCodeSubmitAnswerRequest,
  LeetCodeSubmitAnswerResponse,
  LeetCodeSubmitAnswerResult
}
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.{ LeetCode, LeetCodeCN }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager

enum LeetCodeSearchOrderBy(val value: String) {
  case FontEndId   extends LeetCodeSearchOrderBy("FRONTEND_ID")
  case SolutionNum extends LeetCodeSearchOrderBy("SOLUTION_NUM")
  case ACRate      extends LeetCodeSearchOrderBy("AC_RATE")
  case Difficulty  extends LeetCodeSearchOrderBy("DIFFICULTY")
  case Frequency   extends LeetCodeSearchOrderBy("FREQUENCY")
}

trait LeetCodeApi[F[_]] {
  def getFavoriteList: F[List[LeetCodeFavoriteItem]]
  def getCategoryList: F[List[LeetCodeCategoryListItem]]
  def getTagTypeWithTags: F[List[LeetCodeTagTypeWithTags]]
  def searchChallenges(
    offset: Int,
    limit: Int,
    category: Option[LeetCodeCategoryListItem],
    favorite: Option[LeetCodeFavoriteItem],
    difficulty: Option[ChallengeDifficulty],
    status: Option[ChallengeStatus],
    tags: List[LeetCodeTag],
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
  ): F[LeetCodeChallengeList]

  def searchChallengesWithKeyword(
    offset: Int,
    limit: Int,
    keyword: String,
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
  ): F[LeetCodeChallengeList]

  def getUserInfo: F[LeetCodeUserInfo]

  def checkLogin(): F[Boolean]

  def getQuestionData(slug: String): F[LeetCodeChallengeData]

  def runAnswer(
    id: String,
    slug: String,
    testCase: String,
    language: Language,
    languageVersion: LanguageVersion,
    code: String
  ): Stream[F, LeetCodeRunResult]

  def submitAnswer(
    id: String,
    slug: String,
    language: Language,
    languageVersion: LanguageVersion,
    code: String
  ): Stream[F, LeetCodeSubmitAnswerResult]
}

object LeetCodeApi {
  type LeetCodeDojo = CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
  def apply[F[_]: Async: Concurrent: HttpClientManager](dojo: LeetCodeDojo): LeetCodeApi[F] = new LeetCodeApi[F]
    with Http4sClientDsl[F] {

    private val graphqlUrl = Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/")

    private def useClient[A](fun: Client[F] => F[A]): F[A] = HttpClientManager[F].getClient.use(fun)

    private def commonHeaders(csrfToken: String) =
      Headers("x-csrftoken" -> csrfToken, Referer(Uri.unsafeFromString(s"https://${dojo.domain.toString}/")))

    private def getSubmitAnswerResult(submissionId: Int): F[LeetCodeSubmitAnswerResult] =
      useClient { client =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[LeetCodeSubmitAnswerResult](
              Method.GET(
                Uri.unsafeFromString(s"https://${dojo.domain.toString}/submissions/detail/${submissionId}/check/"),
                headers = commonHeaders(csrfToken)
              )
            )
            .map {
              case r: LeetCodeSubmitAnswerResult.Started => r.copy(leetCodeSubmissionId = submissionId.toString)
              case r: LeetCodeSubmitAnswerResult.Pending => r.copy(leetCodeSubmissionId = submissionId.toString)
              case r                                     => r
            }
        }
      }

    override def submitAnswer(
      id: String,
      slug: String,
      language: Language,
      languageVersion: LanguageVersion,
      code: String
    ): Stream[F, LeetCodeSubmitAnswerResult] =
      Stream
        .eval(useClient { client =>
          getCSRFToken.flatMap { csrfToken =>
            client
              .expect[LeetCodeSubmitAnswerResponse](
                Method
                  .POST(
                    Uri.unsafeFromString(s"https://${dojo.domain.toString}/problems/$slug/submit/"),
                    headers = commonHeaders(csrfToken)
                  )
                  .withEntity(
                    LeetCodeSubmitAnswerRequest(
                      lang = dojo.leetCodeLanguage(language, languageVersion),
                      questionId = id,
                      typedCode = code
                    )
                  )
              )
          }
        })
        .flatMap { response =>
          Stream
            .repeatEval(
              Temporal[F].sleep(1.second) *>
                getSubmitAnswerResult(response.submissionId)
            )
            .flatMap {
              case r: LeetCodeSubmitAnswerResult.Success => Stream(Option(r).widen, None)
              case r                                     => Stream(Option(r).widen)
            }
            .unNoneTerminate
        }

    private def getRunCodeResult(interpretId: String): F[LeetCodeRunResult] =
      useClient { client =>
        getCSRFToken.flatMap { csrfToken =>
          client.expect[LeetCodeRunResult](
            Method.GET(
              Uri.unsafeFromString(s"https://${dojo.domain.toString}/submissions/detail/${interpretId}/check/"),
              headers = commonHeaders(csrfToken)
            )
          )
        }
      }

    override def runAnswer(
      id: String,
      slug: String,
      testCase: String,
      language: Language,
      languageVersion: LanguageVersion,
      code: String
    ): Stream[F, LeetCodeRunResult] = {
      Stream
        .eval(useClient { client =>
          getCSRFToken.flatMap { csrfToken =>
            client
              .expect[LeetCodeRunResponse](
                Method
                  .POST(
                    Uri.unsafeFromString(s"https://${dojo.domain.toString}/problems/$slug/interpret_solution/"),
                    headers = commonHeaders(csrfToken)
                  )
                  .withEntity(
                    LeetCodeRunRequest(
                      lang = dojo.leetCodeLanguage(language, languageVersion),
                      dataInput = testCase,
                      questionId = id,
                      typedCode = code
                    )
                  )
              )
          }
        })
        .flatMap { runResponse =>
          Stream
            .repeatEval(
              Temporal[F].sleep(1.second) *>
                getRunCodeResult(runResponse.interpretId)
            )
            .flatMap {
              case r: LeetCodeRunResult.Success => Stream(Option(r).widen, None)
              case r                            => Stream(Option(r).widen)
            }
            .unNoneTerminate
        }
    }

    override def getFavoriteList: F[List[LeetCodeFavoriteItem]] = HttpClientManager[F].getClient.use { client =>
      client.expect[List[LeetCodeFavoriteItem]](s"https://${dojo.domain.toString}/problems/api/favorites/")
    }

    override def getCategoryList: F[List[LeetCodeCategoryListItem]] = useClient { client =>
      client.expect[Json](s"https://${dojo.domain.toString}/problems/api/card-info/").flatMap { json =>
        JsonPath.root.categories
          .selectDynamic("0")
          .json
          .getOption(json)
          .toRight(ApiError.InvalidContent(dojo, "can not find 'categories.0' in json"))
          .flatMap(_.as[List[LeetCodeCategoryListItem]])
          .liftTo[F]
      }
    }

    override def checkLogin(): F[Boolean] = HttpClientManager[F].getClient.use { client =>
      openGraphQLFile(dojo, "globalData").flatMap { file =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "globalData",
                    query = file,
                    variables = Map.empty[String, String].asJsonObject
                  )
                )
            )
            .flatMap { json =>
              JsonPath.root.data.userStatus.isSignedIn.boolean
                .getOption(json)
                .toRight(ApiError.InvalidContent(dojo, "can not find 'data.userStatus.isSignedIn' in json"))
                .liftTo[F]
            }
        }
      }
    }

    override def getUserInfo: F[LeetCodeUserInfo] = HttpClientManager[F].getClient.use { client =>
      openGraphQLFile(dojo, "globalData").flatMap { file =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[Json](
              Method
                .POST(
                  Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/"),
                  headers = commonHeaders(csrfToken)
                )
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "globalData",
                    query = file,
                    variables = Map.empty[String, String].asJsonObject
                  )
                )
            )
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

    override def getTagTypeWithTags: F[List[LeetCodeTagTypeWithTags]] =
      HttpClientManager[F].getClient.use { client =>
        dojo match
          case LeetCodeCN =>
            getTagTypeWithTagsLeetCodeCN(client)
          case LeetCode =>
            getTagTypeWithTagsLeetCode(client)
      }

    private def getTagTypeWithTagsLeetCode(client: Client[F]): F[List[LeetCodeTagTypeWithTags]] = {
      getCSRFToken.flatMap { csrfToken =>
        client
          .expect[Json](
            Method
              .GET(Uri.unsafeFromString("https://leetcode.com/problems/api/tags/"), headers = commonHeaders(csrfToken))
          )
          .flatMap { json =>
            JsonPath.root.topics.json
              .getOption(json)
              .toRight(ApiError.InvalidContent(dojo, "can not find 'topics' in json"))
              .flatMap { topicsJson =>
                topicsJson.as[List[LeetCodeTag]]
              }
              .map { tags =>
                List(
                  LeetCodeTagTypeWithTags(
                    "All Tags",
                    None,
                    tags.map(tag => LeetCodeTagRelation(tag.questions.length, tag))
                  )
                )
              }
              .liftTo[F]
          }
      }
    }

    private def getTagTypeWithTagsLeetCodeCN(client: Client[F]): F[List[LeetCodeTagTypeWithTags]] = {
      openGraphQLFile(dojo, "questionTagTypeWithTags").flatMap { file =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[Json](
              Method
                .POST(
                  Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/"),
                  headers = commonHeaders(csrfToken)
                )
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "questionTagTypeWithTags",
                    query = file,
                    variables = Map.empty[String, String].asJsonObject
                  )
                )
            )
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

    private def createSearchChallengesFilterJson(
      favorite: Option[LeetCodeFavoriteItem],
      difficulty: Option[ChallengeDifficulty],
      status: Option[ChallengeStatus],
      tags: List[LeetCodeTag],
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
    ): Json = {
      favorite
        .map(item => JsonObject("listId" -> item.id.asJson))
        .getOrElse(JsonObject.empty)
        .deepMerge(
          difficulty
            .map(d => JsonObject("difficulty" -> dojo.leetCodeDifficulty(d).asJson))
            .getOrElse(JsonObject.empty)
            .deepMerge(
              status
                .map(s => JsonObject("status" -> dojo.leetCodeStatus(s).asJson))
                .getOrElse(JsonObject.empty)
                .deepMerge(tags.map(_.slug) match
                  case Nil  => JsonObject.empty
                  case list => JsonObject("tags" -> list.asJson))
            )
        )
        .deepMerge(orderBy.map { case (order, direction) =>
          JsonObject("orderBy" -> order.value.asJson, "sortOrder" -> dojo.leetCodeOrderDirection(direction).asJson)
        }.getOrElse(JsonObject.empty))
        .asJson
    }

    override def searchChallenges(
      offset: Int,
      limit: Int,
      category: Option[LeetCodeCategoryListItem],
      favorite: Option[LeetCodeFavoriteItem],
      difficulty: Option[ChallengeDifficulty],
      status: Option[ChallengeStatus],
      tags: List[LeetCodeTag],
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
    ): F[LeetCodeChallengeList] =
      HttpClientManager[F].getClient.use { client =>
        openGraphQLFile(dojo, "problemsetQuestionList").flatMap { file =>
          getCSRFToken.flatMap { csrfToken =>
            client
              .expect[Json](
                Method
                  .POST(
                    Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/"),
                    headers = commonHeaders(csrfToken)
                  )
                  .withEntity(
                    LeetCodeGraphQLRequest(
                      operationName = "problemsetQuestionList",
                      query = file,
                      variables = Map(
                        "skip"         -> offset.asJson,
                        "limit"        -> limit.asJson,
                        "categorySlug" -> category.map(_.slug).getOrElse("all-code-essentials").asJson,
                        "filters"      -> createSearchChallengesFilterJson(favorite, difficulty, status, tags, orderBy)
                      ).asJsonObject
                    )
                  )
              )
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
    private def createSearchKeywordFilterJson(
      keyWord: String,
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
    ): Json = {
      JsonObject("searchKeywords" -> keyWord.asJson)
        .deepMerge(orderBy.map { case (order, direction) =>
          JsonObject("orderBy" -> order.value.asJson, "sortOrder" -> dojo.leetCodeOrderDirection(direction).asJson)
        }.getOrElse(JsonObject.empty))
        .asJson
    }

    override def searchChallengesWithKeyword(
      offset: Int,
      limit: Int,
      keyword: String,
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
    ): F[LeetCodeChallengeList] = {
      useClient { client =>
        openGraphQLFile(dojo, "problemsetQuestionList").flatMap { file =>
          getCSRFToken.flatMap { csrfToken =>
            client
              .expect[Json](
                Method
                  .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                  .withEntity(
                    LeetCodeGraphQLRequest(
                      operationName = "problemsetQuestionList",
                      query = file,
                      variables = Map(
                        "skip"         -> offset.asJson,
                        "limit"        -> limit.asJson,
                        "categorySlug" -> "all-code-essentials".asJson,
                        "filters"      -> createSearchKeywordFilterJson(keyword, orderBy)
                      ).asJsonObject
                    )
                  )
              )
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

    override def getQuestionData(slug: String): F[LeetCodeChallengeData] = useClient { client =>
      openGraphQLFile(dojo, "questionData").flatMap { queryContent =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "questionData",
                    query = queryContent,
                    variables = Map("titleSlug" -> slug).asJsonObject
                  )
                )
            )
            .flatMap { json =>
              JsonPath.root.data.question.json
                .getOption(json)
                .toRight(ApiError.InvalidContent(dojo, "can not find 'data.question' in json"))
                .flatMap(_.as[LeetCodeChallengeData])
                .liftTo[F]
            }
        }
      }
    }

    private def getCSRFToken: F[String] =
      HttpClientManager[F].findCookieForHost(CIString(dojo.domain.toString), CIString("csrftoken")).map {
        case Some(cookie) => cookie.getValue
        case None         => ""
      }

    def openGraphQLFile(dojo: CodeDojo, fileName: String): F[String] = Resource
      .fromAutoCloseable[F, BufferedSource](
        Async[F]
          .blocking(
            Source.fromInputStream(
              if dojo == CodeDojo.LeetCodeCN then
                Option(getClass.getResourceAsStream(s"/leetcode/graphql/${fileName}_cn.graphql"))
                  .getOrElse(getClass.getResourceAsStream(s"/leetcode/graphql/$fileName.graphql"))
              else getClass.getResourceAsStream(s"/leetcode/graphql/$fileName.graphql")
            )
          )
      )
      .use { bs => Async[F].delay(bs.getLines().mkString(LineSeparator.CR.getSeparatorString)) }
  }
}
