package com.wenjunhuang.codeepiphany.luogu.models

import com.wenjunhuang.codeepiphany.model.OrderDirection
import com.wenjunhuang.codeepiphany.model.OrderDirection.Ascending
import io.circe.*
import org.http4s.Uri
import org.typelevel.ci.CIString

enum LuoGuSearchOrderBy {
  case PID
  case Title
  case Difficulty

  def createOrderBy(uri: Uri, direction: OrderDirection): Uri = this match {
    case PID        => orderDirection(uri.withQueryParam("orderBy", "pid"), direction)
    case Title      => orderDirection(uri.withQueryParam("orderBy", "name"), direction)
    case Difficulty => orderDirection(uri.withQueryParam("orderBy", "difficulty"), direction)
  }

  def orderDirection(uri: Uri, direction: OrderDirection): Uri = direction match
    case Ascending                 => uri.withQueryParam("order", "asc")
    case OrderDirection.Descending => uri.withQueryParam("order", "desc")
}
object LuoGuSearchOrderBy {
  def fromCIString(value: CIString): Option[LuoGuSearchOrderBy] =
    if value == CIString(PID.toString) then Some(PID)
    else if value == CIString(Title.toString) then Some(Title)
    else if value == CIString(Difficulty.toString) then Some(Difficulty)
    else None

  implicit val circeEncoder: Encoder[LuoGuSearchOrderBy] =
    Encoder.encodeString.contramap[LuoGuSearchOrderBy](_.toString)
  implicit val circeDecoder: Decoder[LuoGuSearchOrderBy] =
    Decoder.decodeString.emap(v => fromCIString(CIString(v)).toRight("Unknown LuoGu search order by value"))
}
