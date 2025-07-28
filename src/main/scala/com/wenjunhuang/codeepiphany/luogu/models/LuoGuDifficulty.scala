package com.wenjunhuang.codeepiphany.luogu.models

import io.circe.{Decoder, Encoder}
import com.wenjunhuang.codeepiphany.PluginBundle

enum LuoGuDifficulty(val value: Int) {
  case NonRated          extends LuoGuDifficulty(0)
  case Beginner          extends LuoGuDifficulty(1)
  case BasicMinus        extends LuoGuDifficulty(2)
  case IntermediateMinus extends LuoGuDifficulty(3)
  case IntermediatePlus  extends LuoGuDifficulty(4)
  case AdvancedMinus     extends LuoGuDifficulty(5)
  case ProvincialMinus   extends LuoGuDifficulty(6)
  case NOIPlus           extends LuoGuDifficulty(7)

  def showAsHtml: String = PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
}

object LuoGuDifficulty {
  implicit val circeEncoder: Encoder[LuoGuDifficulty] = Encoder.encodeInt.contramap(_.value)
  implicit val circeDecoder: Decoder[LuoGuDifficulty] = Decoder.decodeInt.emap {
    case 0 => Right(LuoGuDifficulty.NonRated)
    case 1 => Right(LuoGuDifficulty.Beginner)
    case 2 => Right(LuoGuDifficulty.BasicMinus)
    case 3 => Right(LuoGuDifficulty.IntermediateMinus)
    case 4 => Right(LuoGuDifficulty.IntermediatePlus)
    case 5 => Right(LuoGuDifficulty.AdvancedMinus)
    case 6 => Right(LuoGuDifficulty.ProvincialMinus)
    case 7 => Right(LuoGuDifficulty.NOIPlus)
    case _ => Left("Invalid difficulty value")
  }
}
