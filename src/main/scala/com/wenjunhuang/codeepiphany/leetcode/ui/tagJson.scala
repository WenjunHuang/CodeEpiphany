package com.wenjunhuang.codeepiphany.leetcode.ui
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.actions.TagsAction.Tag
import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeTag
import io.circe.*
import io.circe.syntax.*

private def leetCodeTagToJson(tags: List[Tag]): Json = {
  tags
    .map(tag =>
      Json.obj(
        "name"       := tag.name,
        "value"      := tag.value,
        "groupValue" := tag.groupValue,
        "userObj"    := tag.userObj.asInstanceOf[LeetCodeTag]
      )
    )
    .asJson
}

private def leetCodeTagFromJson(json: Json): Either[DecodingFailure, List[Tag]] = {
  json.asArray match {
    case Some(array) =>
      array.map { item =>
        for {
          name       <- item.hcursor.downField("name").as[String]
          value      <- item.hcursor.downField("value").as[String]
          groupValue <- item.hcursor.downField("groupValue").as[String]
          userObj    <- item.hcursor.downField("userObj").as[LeetCodeTag]
        } yield Tag(name, value, groupValue, userObj)
      }.toList.sequence
    case None => Left(DecodingFailure("tags must be an array", Nil))
  }
}
