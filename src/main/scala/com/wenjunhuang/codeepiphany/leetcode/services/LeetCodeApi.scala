package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.{ IO, Resource, Temporal }
import cats.syntax.all.*
import com.intellij.util.LineSeparator
import com.wenjunhuang.codeepiphany.leetcode.models
import com.wenjunhuang.codeepiphany.leetcode.models.*
import com.wenjunhuang.codeepiphany.leetcode.models.runCode.*
import com.wenjunhuang.codeepiphany.leetcode.models.submitAnswer.{
  LeetCodeSubmitAnswerRequest,
  LeetCodeSubmitAnswerResponse,
  LeetCodeSubmitAnswerResult
}
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.{ LeetCode, LeetCodeCN }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import fs2.Stream
import io.circe.*
import io.circe.optics.JsonPath
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.{ Client, UnexpectedStatus }
import org.http4s.headers.Referer
import org.http4s.{ Headers, Method, Uri }
import org.typelevel.ci.CIString

import scala.concurrent.duration.*
import scala.io.{ BufferedSource, Source }

enum LeetCodeSearchOrderBy(val value: String) {
  case FontEndId   extends LeetCodeSearchOrderBy("FRONTEND_ID")
  case SolutionNum extends LeetCodeSearchOrderBy("SOLUTION_NUM")
  case ACRate      extends LeetCodeSearchOrderBy("AC_RATE")
  case Difficulty  extends LeetCodeSearchOrderBy("DIFFICULTY")
  case Frequency   extends LeetCodeSearchOrderBy("FREQUENCY")
}

object LeetCodeSearchOrderBy {
  def fromCIString(value: CIString): Option[LeetCodeSearchOrderBy] =
    if value == CIString(FontEndId.value) then Some(FontEndId)
    else if value == CIString(SolutionNum.value) then Some(SolutionNum)
    else if value == CIString(ACRate.value) then Some(ACRate)
    else if value == CIString(Difficulty.value) then Some(Difficulty)
    else if value == CIString(Frequency.value) then Some(Frequency)
    else None

  implicit val circeEncoder: Encoder[LeetCodeSearchOrderBy] =
    Encoder.encodeString.contramap[LeetCodeSearchOrderBy](_.value)
  implicit val circeDecoder: Decoder[LeetCodeSearchOrderBy] =
    Decoder.decodeString.emap(v => fromCIString(CIString(v)).toRight("Unknown search order by value"))
}

trait LeetCodeApi {
  def getCSRFToken: IO[String]
  def getFavoriteList: IO[List[LeetCodeFavoriteItem]]
  def getCategoryList: IO[List[LeetCodeCategoryListItem]]
  def getTagTypeWithTags: IO[List[LeetCodeTagTypeWithTags]]
  def getCompanyTags: IO[List[LeetCodeProblemsetCompanyTag]]
  def getQuestionCompanyTags: IO[List[LeetCodeQuestionCompanyTag]]
  def getPositionTags: IO[List[LeetCodeProblemsetPositionTag]]
  def searchChallenges(
    offset: Int,
    limit: Int,
    category: Option[LeetCodeCategoryListItem],
    favorite: Option[LeetCodeFavoriteItem],
    difficulty: Option[ChallengeDifficulty],
    status: Option[ChallengeStatus],
    tags: List[LeetCodeTag],
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
  ): IO[LeetCodeChallengeList]
  def searchChallengesWithKeyword(
    offset: Int,
    limit: Int,
    keyword: String,
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
  ): IO[LeetCodeChallengeList]

  def searchCompanyChallenges(
    offset: Int,
    limit: Int,
    interviewPeriodSlug: Option[String],
    companySlugs: List[String],
    positionSlugs: List[String],
    difficulty: Option[ChallengeDifficulty],
    status: Option[ChallengeStatus],
    tags: List[LeetCodeTag],
    orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
  ): IO[LeetCodeCompanyChallengeList]

  def getUserInfo: IO[LeetCodeUserInfo]

  def checkLogin(): IO[Boolean]

  def getQuestionData(slug: String): IO[LeetCodeChallengeData]

  def runAnswer(
    id: String,
    slug: String,
    testCase: String,
    language: Language,
    languageVersion: LanguageVersion,
    code: String
  ): Stream[IO, LeetCodeRunResult]

  def submitAnswer(
    id: String,
    slug: String,
    language: Language,
    languageVersion: LanguageVersion,
    code: String
  ): Stream[IO, LeetCodeSubmitAnswerResult]

  def submitContestAnswer(
    id: String,
    slug: String,
    contestSlug: String,
    language: Language,
    languageVersion: LanguageVersion,
    code: String
  ): Stream[IO, LeetCodeSubmitAnswerResult]

  def getSolutionTags(questionSlug: String): IO[LeetCodeSolutionTags]

  def searchQuestionSolutionArticles(
    offset: Int,
    limit: Int,
    questionSlug: String,
    orderBy: LeetCodeQuestionSolutionArticlesOrderBy,
    userInput: Option[String] = None,
    tagSlugs: List[String] = Nil
  ): IO[LeetCodeQuestionSolutionArticles]

  def getSolutionArticle(articleSlug: String): IO[LeetCodeSolutionArticle]
}

object LeetCodeApi {

  def apply(dojo: LeetCodeDojo): LeetCodeApi = new LeetCodeApi with Http4sClientDsl[IO] {

    private val graphqlUrl = Uri.unsafeFromString(s"https://${dojo.domain.toString}/graphql/")

    private def useClient[A](fun: Client[IO] => IO[A]): IO[A] = HttpClientManager.getClient.use(fun)

    private def commonHeaders(csrfToken: String) =
      Headers("x-csrftoken" -> csrfToken, Referer(Uri.unsafeFromString(s"https://${dojo.domain.toString}/")))

    private def getSubmitAnswerResult(submissionId: Int): IO[LeetCodeSubmitAnswerResult] =
      useClient { client =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[LeetCodeSubmitAnswerResult](
              Method.GET(
                Uri.unsafeFromString(s"https://${dojo.domain.toString}/submissions/detail/$submissionId/check/"),
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

    override def getPositionTags: IO[List[LeetCodeProblemsetPositionTag]] = useClient { client =>
      openGraphQLFile(dojo, "problemsetPositionTags").flatMap { file =>
        getCSRFToken.flatMap { csrfToken =>
          client.expect[Json](
            Method
              .POST(graphqlUrl, headers = commonHeaders(csrfToken))
              .withEntity(
                LeetCodeGraphQLRequest(
                  operationName = "problemsetPositionTags",
                  query = file,
                  variables = Map.empty[String, String].asJsonObject
                )
              )
          )
        }.flatMap { json =>
          JsonPath.root.data.problemsetPositionTags.json
            .getOption(json)
            .toRight(ApiError.InvalidContent(dojo, "can not find 'data.problemsetPositionTags' in json"))
            .flatMap(it => it.as[List[LeetCodeProblemsetPositionTag]])
            .liftTo
        }
      }
    }

    override def getCompanyTags: IO[List[LeetCodeProblemsetCompanyTag]] = useClient { client =>
      openGraphQLFile(dojo, "problemsetCompanyTags").flatMap { file =>
        getCSRFToken.flatMap { csrfToken =>
          client.expect[Json](
            Method
              .POST(graphqlUrl, headers = commonHeaders(csrfToken))
              .withEntity(
                LeetCodeGraphQLRequest(
                  operationName = "problemsetCompanyTags",
                  query = file,
                  variables = Map.empty[String, String].asJsonObject
                )
              )
          )
        }.flatMap { json =>
          JsonPath.root.data.problemsetCompanyTags.json
            .getOption(json)
            .toRight(ApiError.InvalidContent(dojo, "can not find 'data.problemsetCompanyTags' in json"))
            .flatMap(it => it.as[List[LeetCodeProblemsetCompanyTag]])
            .liftTo
        }
      }
    }

    override def getQuestionCompanyTags: IO[List[LeetCodeQuestionCompanyTag]] =
      useClient { client =>
        openGraphQLFile(dojo, "questionCompanyTagsV2").flatMap { file =>
          getCSRFToken.flatMap { csrfToken =>
            client.expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "questionCompanyTagsV2",
                    query = file,
                    variables = Map.empty[String, String].asJsonObject
                  )
                )
            )
          }.flatMap { json =>
            JsonPath.root.data.companyTagsV2.companyTagInfoNodes.json
              .orElse(JsonPath.root.data.companyTags.json)
              .getOption(json)
              .toRight(ApiError.InvalidContent(dojo, "can not find 'data.problemsetCompanyTags' in json"))
              .flatMap(it => it.as[List[LeetCodeQuestionCompanyTag]])
              .liftTo
          }
        }
      }

    override def submitContestAnswer(
      id: String,
      slug: String,
      contestSlug: String,
      language: Language,
      languageVersion: LanguageVersion,
      code: String
    ): Stream[IO, LeetCodeSubmitAnswerResult] = {
      submitAnswer(
        Uri.unsafeFromString(s"https://${dojo.domain.toString}/contest/api/$contestSlug/problems/$slug/submit/"),
        LeetCodeSubmitAnswerRequest(
          lang = dojo.leetCodeLanguage(language, languageVersion),
          questionId = id,
          typedCode = code
        )
      )
    }

    override def submitAnswer(
      id: String,
      slug: String,
      language: Language,
      languageVersion: LanguageVersion,
      code: String
    ): Stream[IO, LeetCodeSubmitAnswerResult] = {
      submitAnswer(
        Uri.unsafeFromString(s"https://${dojo.domain.toString}/problems/$slug/submit/"),
        LeetCodeSubmitAnswerRequest(
          lang = dojo.leetCodeLanguage(language, languageVersion),
          questionId = id,
          typedCode = code
        )
      )
    }

    private def submitAnswer(uri: Uri, request: LeetCodeSubmitAnswerRequest): Stream[IO, LeetCodeSubmitAnswerResult] =
      Stream
        .eval(useClient { client =>
          getCSRFToken.flatMap { csrfToken =>
            client
              .expect[LeetCodeSubmitAnswerResponse](
                Method
                  .POST(uri, headers = commonHeaders(csrfToken))
                  .withEntity(request)
              )
          }
        })
        .flatMap { response =>
          Stream
            .repeatEval(
              Temporal[IO].sleep(1.second) *>
                getSubmitAnswerResult(response.submissionId)
            )
            .flatMap {
              case r: LeetCodeSubmitAnswerResult.Success => Stream(Option(r).widen, None)
              case r                                     => Stream(Option(r).widen)
            }
            .unNoneTerminate
        }

    private def getRunCodeResult(interpretId: String): IO[LeetCodeRunResult] =
      useClient { client =>
        getCSRFToken.flatMap { csrfToken =>
          client.expect[LeetCodeRunResult](
            Method.GET(
              Uri.unsafeFromString(s"https://${dojo.domain.toString}/submissions/detail/$interpretId/check/"),
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
    ): Stream[IO, LeetCodeRunResult] = {
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
              Temporal[IO].sleep(1.second) *>
                getRunCodeResult(runResponse.interpretId)
            )
            .flatMap {
              case r: LeetCodeRunResult.Success => Stream(Option(r).widen, None)
              case r                            => Stream(Option(r).widen)
            }
            .unNoneTerminate
        }
    }

    override def getFavoriteList: IO[List[LeetCodeFavoriteItem]] = HttpClientManager.getClient.use { client =>
      for
        csrfToken <- getCSRFToken
        response <- client.expect[List[LeetCodeFavoriteItem]](
          Method.GET(
            Uri.unsafeFromString(s"https://${dojo.domain.toString}/problems/api/favorites/"),
            headers = commonHeaders(csrfToken)
          )
        )
      yield response
    }

    override def getCategoryList: IO[List[LeetCodeCategoryListItem]] = useClient { client =>
      getCSRFToken.flatMap { csrfToken =>
        client
          .expect[Json](
            Method.GET(
              Uri.unsafeFromString(s"https://${dojo.domain.toString}/problems/api/card-info/"),
              headers = commonHeaders(csrfToken)
            )
          )
          .flatMap { json =>
            JsonPath.root.categories
              .selectDynamic("0")
              .json
              .getOption(json)
              .toRight(ApiError.InvalidContent(dojo, "can not find 'categories.0' in json"))
              .flatMap(_.as[List[LeetCodeCategoryListItem]])
              .liftTo
          }
      }
    }

    override def checkLogin(): IO[Boolean] = HttpClientManager.getClient.use { client =>
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
                .liftTo
            }
            .handleErrorWith { case _: UnexpectedStatus =>
              IO.pure(false)
            }
        }
      }
    }

    override def getUserInfo: IO[LeetCodeUserInfo] = HttpClientManager.getClient.use { client =>
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
                .liftTo
            }
        }
      }
    }

    override def getTagTypeWithTags: IO[List[LeetCodeTagTypeWithTags]] =
      HttpClientManager.getClient.use { client =>
        dojo match
          case LeetCodeCN =>
            getTagTypeWithTagsLeetCodeCN(client)
          case LeetCode =>
            getTagTypeWithTagsLeetCode(client)
      }

    private def getTagTypeWithTagsLeetCode(client: Client[IO]): IO[List[LeetCodeTagTypeWithTags]] = {
      getCSRFToken.flatMap { csrfToken =>
        client
          .expect[Json](
            Method
              .GET(
                Uri.unsafeFromString(s"https://${dojo.domain}/problems/api/tags/"),
                headers = commonHeaders(csrfToken)
              )
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
              .liftTo
          }
      }
    }

    private def getTagTypeWithTagsLeetCodeCN(client: Client[IO]): IO[List[LeetCodeTagTypeWithTags]] = {
      openGraphQLFile(dojo, "questionTagTypeWithTags").flatMap { file =>
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
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
                .liftTo
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
        .deepMerge(createOrderBy(orderBy))
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
    ): IO[LeetCodeChallengeList] = {
      if (dojo == CodeDojo.LeetCode && favorite.nonEmpty) {
        searchCompanyChallenges(
          offset,
          limit,
          None,
          List(favorite.map(_.id).get),
          Nil,
          difficulty,
          status,
          tags,
          orderBy
        ).map { result =>
          LeetCodeChallengeList(
            questions = result.questions.map { q =>
              LeetCodeChallengeListItem(
                acRate = q.acRate,
                difficulty = q.difficulty,
                freqBar = q.freqBar,
                paidOnly = q.paidOnly,
                solutionNum = None,
                status = q.status,
                frontendQuestionId = q.questionFrontendId,
                title = q.title,
                titleCn = None,
                titleSlug = q.titleSlug
              )
            },
            total = result.total
          )
        }

      } else {
        val fileName = dojo match {
          case CodeDojo.LeetCodeCN => "problemsetQuestionList"
          case CodeDojo.LeetCode   => "problemsetQuestionListV2"
        }
        useClient { client =>
          openGraphQLFile(dojo, fileName).flatMap { file =>
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
                        operationName = fileName,
                        query = file,
                        variables = dojo match {
                          case CodeDojo.LeetCodeCN =>
                            Map(
                              "skip"         -> offset.asJson,
                              "limit"        -> limit.asJson,
                              "categorySlug" -> category.map(_.slug).getOrElse("all-code-essentials").asJson,
                              "filters" -> createSearchChallengesFilterJson(favorite, difficulty, status, tags, orderBy)
                            ).asJsonObject
                          case CodeDojo.LeetCode =>
                            Map(
                              "skip"         -> offset.asJson,
                              "limit"        -> limit.asJson,
                              "categorySlug" -> category.map(_.slug).getOrElse("all-code-essentials").asJson,
                              "filters" -> Map(
                                "companyFilter" -> Map(
                                  "companySlugs" -> List.empty[String].asJson,
                                  "operator"     -> "IS".asJson
                                ).asJson,
                                "positionFilter" -> Map(
                                  "positionSlugs" -> List.empty[String].asJson,
                                  "operator"      -> "IS".asJson
                                ).asJson,
                                "acceptanceFilter" -> Map.empty[String, String].asJson,
                                "frequencyFilter"  -> Map.empty[String, String].asJson,
                                "languageFilter" -> Map(
                                  "languageSlugs" -> List.empty[String].asJson,
                                  "operator"      -> "IS".asJson
                                ).asJson,
                                "difficultyFilter" -> Map(
                                  "difficulties" -> difficulty.map(dojo.leetCodeDifficulty).toList.asJson,
                                  "operator"     -> "IS".asJson
                                ).asJson,
                                "premiumFilter" -> Map(
                                  "premiumStatus" -> List.empty[String].asJson,
                                  "operator"      -> "IS".asJson
                                ).asJson,
                                "statusFilter" -> Map(
                                  "questionStatuses" -> status.map(dojo.leetCodeStatusForCompanySearch).toList.asJson,
                                  "operator"         -> "IS".asJson
                                ).asJson,
                                "topicFilter" -> Map(
                                  "topicSlugs" -> tags.map(_.slug).asJson,
                                  "operator"   -> "IS".asJson
                                ).asJson,
                                "filterCombineType" -> "ALL".asJson
                              ).asJson,
                              "sortBy" -> createSortField(
                                orderBy,
                                Map("sortField" -> "CUSTOM".asJson, "sortOrder" -> "ASCENDING".asJson).asJsonObject
                              ).asJson,
                              "searchKeyword" -> "".asJson
                            ).asJsonObject
                        }
                      )
                    )
                )
                .flatMap { json =>
                  JsonPath.root.data
                    .selectDynamic(fileName)
                    .json
                    .getOption(json)
                    .toRight(ApiError.InvalidContent(dojo, s"can not find ${fileName} in json"))
                    .flatMap { json =>
                      dojo match {
                        case CodeDojo.LeetCodeCN =>
                          json.as[LeetCodeChallengeList]
                        case CodeDojo.LeetCode =>
                          json.as[LeetCodeChallengeListV2].map(_.toV1)
                      }
                    }
                    .liftTo
                }
            }
          }
        }
      }
    }

    private def createOrderBy(
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)],
      orElse: JsonObject = JsonObject.empty
    ): JsonObject = {
      orderBy.map { case (order, direction) =>
        JsonObject("orderBy" -> order.value.asJson, "sortOrder" -> dojo.leetCodeOrderDirection(direction).asJson)
      }.getOrElse(orElse)
    }

    private def createSortField(
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)],
      orElse: JsonObject = JsonObject.empty
    ): JsonObject = {
      orderBy.map { case (order, direction) =>
        JsonObject("sortField" -> order.value.asJson, "sortOrder" -> dojo.leetCodeOrderDirection(direction).asJson)
      }.getOrElse(orElse)
    }

    private def createSearchKeywordFilterJson(
      keyWord: String,
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
    ): Json = {
      JsonObject("searchKeywords" -> keyWord.asJson)
        .deepMerge(createOrderBy(orderBy))
        .asJson
    }

    override def searchCompanyChallenges(
      offset: Int,
      limit: Int,
      interviewPeriodSlug: Option[String],
      companySlugs: List[String],
      positionSlugs: List[String],
      difficulty: Option[ChallengeDifficulty],
      status: Option[ChallengeStatus],
      tags: List[LeetCodeTag],
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
    ): IO[LeetCodeCompanyChallengeList] = useClient { client =>
      openGraphQLFile(dojo, "favoriteQuestionList").flatMap { file =>
        val favorite = companySlugs.head
        val rest     = companySlugs.tail
        getCSRFToken.flatMap { csrfToken =>
          client
            .expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "favoriteQuestionList",
                    query = file,
                    variables = Map(
                      "skip"         -> offset.asJson,
                      "limit"        -> limit.asJson,
                      "favoriteSlug" -> interviewPeriodSlug.map(it => s"$favorite-$it").getOrElse(favorite).asJson,
                      "filtersV2" -> Map(
                        "companyFilter" -> Map("companySlugs" -> rest.asJson, "operator" -> "IS".asJson).asJson,
                        "positionFilter" -> Map(
                          "positionSlugs" -> positionSlugs.asJson,
                          "operator"      -> "IS".asJson
                        ).asJson,
                        "acceptanceFilter" -> Map.empty[String, String].asJson,
                        "frequencyFilter"  -> Map.empty[String, String].asJson,
                        "languageFilter" -> Map(
                          "languageSlugs" -> List.empty[String].asJson,
                          "operator"      -> "IS".asJson
                        ).asJson,
                        "difficultyFilter" -> Map(
                          "difficulties" -> difficulty.map(dojo.leetCodeDifficulty).toList.asJson,
                          "operator"     -> "IS".asJson
                        ).asJson,
                        "premiumFilter" -> Map(
                          "premiumStatus" -> List.empty[String].asJson,
                          "operator"      -> "IS".asJson
                        ).asJson,
                        "statusFilter" -> Map(
                          "questionStatuses" -> status.map(dojo.leetCodeStatusForCompanySearch).toList.asJson,
                          "operator"         -> "IS".asJson
                        ).asJson,
                        "topicFilter" -> Map("topicSlugs" -> tags.map(_.slug).asJson, "operator" -> "IS".asJson).asJson,
                        "filterCombineType" -> "ALL".asJson
                      ).asJson,
                      "sortBy" -> createSortField(
                        orderBy,
                        Map("sortField" -> "CUSTOM".asJson, "sortOrder" -> "ASCENDING".asJson).asJsonObject
                      ).asJson,
                      "searchKeyword" -> "".asJson
                    ).asJsonObject
                  )
                )
            )
            .flatMap { json =>
              JsonPath.root.data.favoriteQuestionList.json
                .getOption(json)
                .toRight(ApiError.InvalidContent(dojo, "can not find 'favoriteQuestionList' in json"))
                .flatMap(_.as[LeetCodeCompanyChallengeList])
                .liftTo
            }
        }
      }
    }

    override def searchChallengesWithKeyword(
      offset: Int,
      limit: Int,
      keyword: String,
      orderBy: Option[(LeetCodeSearchOrderBy, OrderDirection)]
    ): IO[LeetCodeChallengeList] = {
      val fileName = dojo match {
        case CodeDojo.LeetCodeCN => "problemsetQuestionList"
        case CodeDojo.LeetCode   => "problemsetQuestionListV2"
      }
      useClient { client =>
        openGraphQLFile(dojo, fileName).flatMap { file =>
          getCSRFToken.flatMap { csrfToken =>
            client
              .expect[Json](
                Method
                  .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                  .withEntity(
                    LeetCodeGraphQLRequest(
                      operationName = fileName,
                      query = file,
                      variables = dojo match {
                        case CodeDojo.LeetCodeCN =>
                          Map(
                            "skip"         -> offset.asJson,
                            "limit"        -> limit.asJson,
                            "categorySlug" -> "all-code-essentials".asJson,
                            "filters"      -> createSearchKeywordFilterJson(keyword, orderBy)
                          ).asJsonObject
                        case CodeDojo.LeetCode =>
                          Map(
                            "skip"         -> offset.asJson,
                            "limit"        -> limit.asJson,
                            "categorySlug" -> "all-code-essentials".asJson,
                            "filters" -> Map(
                              "companyFilter" -> Map(
                                "companySlugs" -> List.empty[String].asJson,
                                "operator"     -> "IS".asJson
                              ).asJson,
                              "positionFilter" -> Map(
                                "positionSlugs" -> List.empty[String].asJson,
                                "operator"      -> "IS".asJson
                              ).asJson,
                              "acceptanceFilter" -> Map.empty[String, String].asJson,
                              "frequencyFilter"  -> Map.empty[String, String].asJson,
                              "languageFilter" -> Map(
                                "languageSlugs" -> List.empty[String].asJson,
                                "operator"      -> "IS".asJson
                              ).asJson,
                              "difficultyFilter" -> Map(
                                "difficulties" -> List.empty[String].asJson,
                                "operator"     -> "IS".asJson
                              ).asJson,
                              "premiumFilter" -> Map(
                                "premiumStatus" -> List.empty[String].asJson,
                                "operator"      -> "IS".asJson
                              ).asJson,
                              "statusFilter" -> Map(
                                "questionStatuses" -> List.empty[String].asJson,
                                "operator"         -> "IS".asJson
                              ).asJson,
                              "topicFilter" -> Map(
                                "topicSlugs" -> List.empty[String].asJson,
                                "operator"   -> "IS".asJson
                              ).asJson,
                              "filterCombineType" -> "ALL".asJson
                            ).asJson,
                            "sortBy" -> createSortField(
                              orderBy,
                              Map("sortField" -> "CUSTOM".asJson, "sortOrder" -> "ASCENDING".asJson).asJsonObject
                            ).asJson,
                            "searchKeyword" -> keyword.asJson
                          ).asJsonObject
                      }
                    )
                  )
              )
              .flatMap { json =>
                JsonPath.root.data.selectDynamic(fileName).json
                  .getOption(json)
                  .toRight(ApiError.InvalidContent(dojo, s"can not find '${fileName}' in json"))
                  .flatMap { json =>
                    dojo match {
                      case CodeDojo.LeetCodeCN =>
                        json.as[LeetCodeChallengeList]
                      case CodeDojo.LeetCode =>
                        json.as[LeetCodeChallengeListV2].map(_.toV1)
                    }
                  }
                  .liftTo
              }
          }

        }
      }
    }

    override def getQuestionData(slug: String): IO[LeetCodeChallengeData] = useClient { client =>
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
                .liftTo
            }
        }
      }
    }

    override def getCSRFToken: IO[String] =
      useClient { client =>
        client.get[String](Uri.unsafeFromString(s"https://${dojo.domain.toString}/api/home/")) { response =>
          response.cookies.find(_.name == "csrftoken") match
            case Some(cookie) => IO.delay(cookie.content)
            case None =>
              IO.delay("")
        }
      }

    def openGraphQLFile(dojo: CodeDojo, fileName: String): IO[String] = Resource
      .fromAutoCloseable[IO, BufferedSource](
        IO
          .blocking(
            Source.fromInputStream(
              if dojo == CodeDojo.LeetCodeCN then
                Option(getClass.getResourceAsStream(s"/leetcode/graphql/${fileName}_cn.graphql"))
                  .getOrElse(getClass.getResourceAsStream(s"/leetcode/graphql/$fileName.graphql"))
              else getClass.getResourceAsStream(s"/leetcode/graphql/$fileName.graphql")
            )
          )
      )
      .use { bs => IO.delay(bs.getLines().mkString(LineSeparator.CR.getSeparatorString)) }

    override def getSolutionTags(questionSlug: String): IO[LeetCodeSolutionTags] = {
      if dojo == CodeDojo.LeetCodeCN then
        useClient { client =>
          for
            csrfToken <- getCSRFToken
            file      <- openGraphQLFile(dojo, "solutionTags")
            response <- client.expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "solutionTags",
                    query = file,
                    variables = Map("questionSlug" -> questionSlug).asJsonObject
                  )
                )
            )
          yield JsonPath.root.data.solutionTags.json.getOption(response) match
            case Some(json) =>
              json.as[LeetCodeSolutionTags] match
                case Right(tags) => tags
                case Left(e)     => throw ApiError.InvalidContent(dojo, e.message)
            case None =>
              throw ApiError.InvalidContent(dojo, "can not find 'data.solutionTags' in json")
        }
      else
        useClient { client =>
          for
            csrfToken <- getCSRFToken
            file      <- openGraphQLFile(dojo, "ugcArticleSolutionTags")
            response <- client.expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "ugcArticleSolutionTags",
                    query = file,
                    variables = Map("questionSlug" -> questionSlug).asJsonObject
                  )
                )
            )
          yield JsonPath.root.data.ugcArticleSolutionTags.json.getOption(response) match
            case Some(json) =>
              json.as[LeetCodeSolutionTags] match
                case Right(tags) => tags
                case Left(e)     => throw ApiError.InvalidContent(dojo, e.message)
            case None =>
              throw ApiError.InvalidContent(dojo, "can not find 'data.solutionTags' in json")
        }

    }

    override def searchQuestionSolutionArticles(
      offset: Int,
      limit: Int,
      questionSlug: String,
      orderBy: LeetCodeQuestionSolutionArticlesOrderBy,
      userInput: Option[String],
      tagSlugs: List[String]
    ): IO[LeetCodeQuestionSolutionArticles] = useClient { client =>
      if dojo == CodeDojo.LeetCodeCN then
        openGraphQLFile(dojo, "questionTopicsList").flatMap { file =>
          getCSRFToken.flatMap { csrfToken =>
            client.expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "questionTopicsList",
                    query = file,
                    variables = Map(
                      "skip"         -> offset.asJson,
                      "first"        -> limit.asJson,
                      "questionSlug" -> questionSlug.asJson,
                      "orderBy"      -> orderBy.leetCodeCN.asJson,
                      "userInput"    -> userInput.getOrElse("").asJson,
                      "tagSlugs"     -> tagSlugs.asJson
                    ).asJsonObject
                  )
                )
            )
          }.flatMap { json =>
            JsonPath.root.data.questionSolutionArticles.json
              .getOption(json)
              .toRight(ApiError.InvalidContent(dojo, "can not find 'data.questionSolutionArticles' in json"))
              .flatMap(_.as[LeetCodeQuestionSolutionArticles])
              .liftTo
          }
        }
      else
        openGraphQLFile(dojo, "ugcArticleSolutionArticles").flatMap { file =>
          getCSRFToken.flatMap { csrfToken =>
            client.expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "ugcArticleSolutionArticles",
                    query = file,
                    variables = Map(
                      "skip"         -> offset.asJson,
                      "first"        -> limit.asJson,
                      "questionSlug" -> questionSlug.asJson,
                      "orderBy"      -> orderBy.leetCode.map(_.asJson).getOrElse(Json.Null),
                      "userInput"    -> userInput.getOrElse("").asJson,
                      "tagSlugs"     -> tagSlugs.asJson
                    ).asJsonObject
                  )
                )
            )
          }.flatMap { json =>
            JsonPath.root.data.ugcArticleSolutionArticles.json
              .getOption(json)
              .toRight(ApiError.InvalidContent(dojo, "can not find 'data.ugcArticleSolutionArticles' in json"))
              .flatMap(_.as[LeetCodeQuestionSolutionArticles])
              .liftTo

          }
        }
    }

    override def getSolutionArticle(articleSlug: String): IO[LeetCodeSolutionArticle] = useClient { client =>
      if dojo == CodeDojo.LeetCodeCN then
        openGraphQLFile(dojo, "discussTopic").flatMap { file =>
          getCSRFToken.flatMap { csrfToken =>
            client.expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "discussTopic",
                    query = file,
                    variables = Map("slug" -> articleSlug).asJsonObject
                  )
                )
            )
          }.flatMap { json =>
            JsonPath.root.data.solutionArticle.json
              .getOption(json)
              .toRight(ApiError.InvalidContent(dojo, "can not find 'data.solutionArticle' in json"))
              .flatMap(_.as[LeetCodeSolutionArticle])
              .liftTo
          }
        }
      else
        openGraphQLFile(dojo, "ugcArticleSolutionArticle").flatMap { file =>
          getCSRFToken.flatMap { csrfToken =>
            client.expect[Json](
              Method
                .POST(graphqlUrl, headers = commonHeaders(csrfToken))
                .withEntity(
                  LeetCodeGraphQLRequest(
                    operationName = "ugcArticleSolutionArticle",
                    query = file,
                    variables = Map("slug" -> articleSlug).asJsonObject
                  )
                )
            )
          }.flatMap { json =>
            JsonPath.root.data.ugcArticleSolutionArticle.json
              .getOption(json)
              .toRight(ApiError.InvalidContent(dojo, "can not find 'data.ugcArticleSolutionArticle' in json"))
              .flatMap(_.as[LeetCodeSolutionArticle])
              .liftTo
          }
        }
    }
  }
}
