package com.wenjunhuang.codeepiphany.model

import org.jooq.{ Field, SortField }
import org.typelevel.ci.CIString
import io.circe.*

enum OrderDirection {
  case Ascending
  case Descending

  def toJooqSortField[T](field: Field[T]): SortField[T] = this match
    case Ascending  => field.asc()
    case Descending => field.desc()

}

object OrderDirection {
  def fromCIString(value: CIString): Option[OrderDirection] =
    if value == CIString(Ascending.toString) then Some(Ascending)
    else if value == CIString(Descending.toString) then Some(Descending)
    else None

  implicit val circeEncoder: Encoder[OrderDirection] = Encoder.encodeString.contramap[OrderDirection](_.toString)
  implicit val circeDecoder: Decoder[OrderDirection] =
    Decoder.decodeString.emap(v => fromCIString(CIString(v)).toRight("Unknown order direction value"))
}
