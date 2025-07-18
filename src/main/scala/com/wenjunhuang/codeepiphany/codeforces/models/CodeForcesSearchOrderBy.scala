package com.wenjunhuang.codeepiphany.codeforces.models

import io.circe.{Decoder, Encoder}
import org.typelevel.ci.CIString

enum CodeForcesSearchOrderBy {
  case Rating
  case ContestIdIndex
}

object CodeForcesSearchOrderBy {
  def fromCIString(value: CIString): Option[CodeForcesSearchOrderBy] =
    if value == CIString(Rating.toString) then Some(Rating)
    else if value == CIString(ContestIdIndex.toString) then Some(ContestIdIndex)
    else None

  implicit val circeEncoder: Encoder[CodeForcesSearchOrderBy] =
    Encoder.encodeString.contramap[CodeForcesSearchOrderBy](_.toString)
  implicit val circeDecoder: Decoder[CodeForcesSearchOrderBy] =
    Decoder.decodeString.emap(v => fromCIString(CIString(v)).toRight("Unknown order by value"))

}
