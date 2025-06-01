package com.wenjunhuang.codeepiphany.leetcode.models

import io.circe.derivation.ConfiguredCodec
import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject }
import io.circe.Decoder.Result

case class LeetCodeQuestionSolutionArticleTag(
  name: String,
  nameTranslated: Option[String] = None,
  slug: String,
  tagType: Option[String] = None
) derives ConfiguredCodec

case class LeetCodeQuestionSolutionArticleReaction(count: Int, reaction: String) derives ConfiguredCodec

case class LeetCodeQuestionSolutionArticleAuthor(
  username: String,
  certificationLevel: String,
  userAvatar: String,
  userSlug: String,
  realName: String
)

object LeetCodeQuestionSolutionArticleAuthor {
  implicit val decoder: Decoder[LeetCodeQuestionSolutionArticleAuthor] = (c: HCursor) => {
    def getField(field: String) = c.downField(field).as[String].orElse {
      c.downField(field)
        .as[JsonObject]
        .flatMap(_.apply(field).flatMap(_.asString).toRight(DecodingFailure(s"Missing $field", c.history)))
    }

    for {
      username           <- c.downField("username").as[String]
      certificationLevel <- c.downField("certificationLevel").as[String]
      userAvatar         <- getField("userAvatar")
      userSlug           <- getField("userSlug")
      realName           <- getField("realName")
    } yield LeetCodeQuestionSolutionArticleAuthor(username, certificationLevel, userAvatar, userSlug, realName)
  }

  implicit val encoder: Encoder[LeetCodeQuestionSolutionArticleAuthor] =
    (author: LeetCodeQuestionSolutionArticleAuthor) => {
      JsonObject(
        "username"           -> Json.fromString(author.username),
        "certificationLevel" -> Json.fromString(author.certificationLevel),
        "userAvatar"         -> Json.fromString(author.userAvatar),
        "userSlug"           -> Json.fromString(author.userSlug),
        "realName"           -> Json.fromString(author.realName)
      ).toJson
    }
}

case class LeetCodeQuestionSolutionArticleTopic(id: Int, commentCount: Int) derives ConfiguredCodec

case class LeetCodeQuestionSolutionArticleVideoInfo(videoId: String, coverUrl: String, duration: Double)
    derives ConfiguredCodec

case class LeetCodeQuestionSolutionArticle(
  uuid: String,
  title: String,
  slug: String,
  hasVideo: Boolean,
  reactions: List[LeetCodeQuestionSolutionArticleReaction],
  tags: List[LeetCodeQuestionSolutionArticleTag],
  createdAt: String,
  author: LeetCodeQuestionSolutionArticleAuthor,
  summary: String,
  topic: LeetCodeQuestionSolutionArticleTopic,
  byLeetcode: Boolean,
  isMyFavorite: Boolean,
  chargeType: String,
  isEditorsPick: Option[Boolean] = None,
  hitCount: Int,
  videosInfo: List[LeetCodeQuestionSolutionArticleVideoInfo] = Nil
) derives ConfiguredCodec

case class LeetCodeQuestionSolutionArticleEdge(node: LeetCodeQuestionSolutionArticle) derives ConfiguredCodec

case class LeetCodeQuestionSolutionArticles(edges: List[LeetCodeQuestionSolutionArticleEdge], totalNum: Int)
    derives ConfiguredCodec

enum LeetCodeQuestionSolutionArticlesOrderBy(val leetCodeCN: String, val leetCode: Option[String]) {
  case MostUpvote extends LeetCodeQuestionSolutionArticlesOrderBy("MOST_UPVOTE", Some("MOST_UPVOTE"))
  case Hot        extends LeetCodeQuestionSolutionArticlesOrderBy("HOT", Some("HOT"))
  case Newest     extends LeetCodeQuestionSolutionArticlesOrderBy("NEWEST_TO_OLDEST", Some("MOST_RECENT"))
  case Oldest     extends LeetCodeQuestionSolutionArticlesOrderBy("OLDEST_TO_NEWEST", None)
}
