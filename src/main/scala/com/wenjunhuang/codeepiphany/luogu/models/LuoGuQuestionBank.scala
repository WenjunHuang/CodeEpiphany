package com.wenjunhuang.codeepiphany.luogu.models
import cats.Show
import io.circe.{Decoder, Encoder}

enum LuoGuQuestionBank(val value: String) {
  case BP  extends LuoGuQuestionBank("B|P")
  case P   extends LuoGuQuestionBank("P")
  case B   extends LuoGuQuestionBank("B")
  case CF  extends LuoGuQuestionBank("CF")
  case SP  extends LuoGuQuestionBank("SP")
  case AT  extends LuoGuQuestionBank("AT")
  case UVA extends LuoGuQuestionBank("UVA")
}

object LuoGuQuestionBank {
  implicit val show: Show[LuoGuQuestionBank] = Show.show[LuoGuQuestionBank] {
    case LuoGuQuestionBank.BP  => "洛谷"
    case LuoGuQuestionBank.P   => "主题库"
    case LuoGuQuestionBank.B   => "入门与面试"
    case LuoGuQuestionBank.CF  => "CodeForces"
    case LuoGuQuestionBank.SP  => "SPOJ"
    case LuoGuQuestionBank.AT  => "AtCoder"
    case LuoGuQuestionBank.UVA => "UVA"
  }

  implicit val circeEncoder: Encoder[LuoGuQuestionBank] = Encoder.encodeString.contramap(_.value)
  implicit val circeDecoder: Decoder[LuoGuQuestionBank] = Decoder.decodeString.emap {
    case "B|P" => Right(LuoGuQuestionBank.BP)
    case "P"   => Right(LuoGuQuestionBank.P)
    case "B"   => Right(LuoGuQuestionBank.B)
    case "CF"  => Right(LuoGuQuestionBank.CF)
    case "SP"  => Right(LuoGuQuestionBank.SP)
    case "AT"  => Right(LuoGuQuestionBank.AT)
    case "UVA" => Right(LuoGuQuestionBank.UVA)
    case _     => Left("Invalid luogu challenge type value")
  }
}
