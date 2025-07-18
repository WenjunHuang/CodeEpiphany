package com.wenjunhuang.codeepiphany.luogu.ui

import com.wenjunhuang.codeepiphany.luogu.models.LuoGuTag
import io.circe.*
import io.circe.syntax.*
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.actions.TagsAction.Tag

private def luoguTagToJson(tags: List[Tag]): Json = {
  tags
    .map(tag =>
      Json.obj(
        "name"       := tag.name,
        "value"      := tag.value,
        "groupValue" := tag.groupValue,
        "userObj"    := tag.userObj.asInstanceOf[LuoGuTag]
      )
    )
    .asJson
}

private def luoguTagFromJson(json: Json): Either[DecodingFailure, List[Tag]] = {
  json.asArray match {
    case Some(array) =>
      array.map { item =>
        for {
          name       <- item.hcursor.downField("name").as[String]
          value      <- item.hcursor.downField("value").as[String]
          groupValue <- item.hcursor.downField("groupValue").as[String]
          userObj    <- item.hcursor.downField("userObj").as[LuoGuTag]
        } yield Tag(name, value, groupValue, userObj)
      }.toList.sequence
    case None => Left(DecodingFailure("tags must be an array", Nil))
  }
}
