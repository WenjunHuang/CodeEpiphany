package com.wenjunhuang.codeepiphany.atcoder.models
import io.circe.*
import org.typelevel.ci.CIString

enum AtCoderSearchOrderBy {
  case Difficulty
  case ContestId
}

object AtCoderSearchOrderBy {
  def fromCIString(value: CIString): Option[AtCoderSearchOrderBy] =
    if value == CIString(Difficulty.toString) then Some(Difficulty)
    else if value == CIString(ContestId.toString) then Some(ContestId)
    else None

  implicit val circeEncoder: Encoder[AtCoderSearchOrderBy] =
    Encoder.encodeString.contramap[AtCoderSearchOrderBy](_.toString)
  implicit val circeDecoder: Decoder[AtCoderSearchOrderBy] =
    Decoder.decodeString.emap(v => fromCIString(CIString(v)).toRight("Unknown AtCoder search order by value"))

}
