package com.wenjunhuang.codeepiphany.codeforces.ui

import io.circe.*
import io.circe.syntax.*
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.actions.TagsAction.Tag

private def codeForcesTagToJson(tags: List[Tag]): Json = {
  tags
    .map(tag => Json.obj("name" := tag.name, "value" := tag.value, "groupValue" := tag.groupValue))
    .asJson
}

private def codeForcesTagFromJson(json: Json): Either[DecodingFailure, List[Tag]] = {
  json.asArray match {
    case Some(array) =>
      array.map { item =>
        for {
          name       <- item.hcursor.downField("name").as[String]
          value      <- item.hcursor.downField("value").as[String]
          groupValue <- item.hcursor.downField("groupValue").as[String]
        } yield Tag(name, value, groupValue, null)
      }.toList.sequence
    case None => Left(DecodingFailure("tags must be an array", Nil))
  }
}
