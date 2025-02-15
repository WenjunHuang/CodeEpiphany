package com.wenjunhuang.codeepiphany.luogu.models
import cats.syntax.all.*
import cats.Show
import io.circe.{Decoder, Encoder}

enum LuoGuChallengeType(val value: String) {
  case BP  extends LuoGuChallengeType("B|P")
  case P   extends LuoGuChallengeType("P")
  case B   extends LuoGuChallengeType("B")
  case CF  extends LuoGuChallengeType("CF")
  case SP  extends LuoGuChallengeType("SP")
  case AT  extends LuoGuChallengeType("AT")
  case UVA extends LuoGuChallengeType("UVA")
}

object LuoGuChallengeType {
  implicit val show: Show[LuoGuChallengeType] = Show.show[LuoGuChallengeType] {
    case LuoGuChallengeType.BP  => "洛谷"
    case LuoGuChallengeType.P   => "主题库"
    case LuoGuChallengeType.B   => "入门与面试"
    case LuoGuChallengeType.CF  => "CodeForces"
    case LuoGuChallengeType.SP  => "SPOJ"
    case LuoGuChallengeType.AT  => "AtCoder"
    case LuoGuChallengeType.UVA => "UVA"
  }

  implicit val circeEncoder: Encoder[LuoGuChallengeType] = Encoder.encodeString.contramap(_.value)
  implicit val circeDecoder: Decoder[LuoGuChallengeType] = Decoder.decodeString.emap {
    case "B|P" => Right(LuoGuChallengeType.BP)
    case "P"   => Right(LuoGuChallengeType.P)
    case "B"   => Right(LuoGuChallengeType.B)
    case "CF"  => Right(LuoGuChallengeType.CF)
    case "SP"  => Right(LuoGuChallengeType.SP)
    case "AT"  => Right(LuoGuChallengeType.AT)
    case "UVA" => Right(LuoGuChallengeType.UVA)
    case _     => Left("Invalid luogu challenge type value")
  }
}
