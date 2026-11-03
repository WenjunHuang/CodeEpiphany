package com.wenjunhuang.codeepiphany.luogu.models

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.DifficultyColors
import io.circe.{ Decoder, Encoder }

enum LuoGuDifficulty(val value: Int) {
  case NonRated     extends LuoGuDifficulty(0)
  case Beginner     extends LuoGuDifficulty(1)
  case BasicMinus   extends LuoGuDifficulty(2)
  case Basic        extends LuoGuDifficulty(3)
  case BasicPlus    extends LuoGuDifficulty(4)
  case Advanced     extends LuoGuDifficulty(5)
  case AdvancedPlus extends LuoGuDifficulty(6)
  case NOIMinus     extends LuoGuDifficulty(7)
  case NOIPlus      extends LuoGuDifficulty(8)

  def showAsHtml: String = {
    this match {
      case NonRated => PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
      case Beginner =>
        DifficultyColors.makeColorHtml(
          DifficultyColors.DIFFICULTY_EASY_COLOR,
          PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
        )
      case BasicMinus =>
        DifficultyColors.makeColorHtml(
          DifficultyColors.DIFFICULTY_MEDIUM_COLOR,
          PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
        )
      case Basic =>
        DifficultyColors.makeColorHtml(
          DifficultyColors.DIFFICULTY_MEDIUM_COLOR,
          PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
        )
      case BasicPlus =>
        DifficultyColors.makeColorHtml(
          DifficultyColors.DIFFICULTY_MEDIUM_COLOR,
          PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
        )
      case Advanced =>
        DifficultyColors.makeColorHtml(
          DifficultyColors.DIFFICULTY_ADVANCED_COLOR,
          PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
        )
      case AdvancedPlus =>
        DifficultyColors.makeColorHtml(
          DifficultyColors.DIFFICULTY_ADVANCED_COLOR,
          PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
        )
      case NOIMinus =>
        DifficultyColors.makeColorHtml(
          DifficultyColors.DIFFICULTY_EXPERT_COLOR,
          PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
        )
      case NOIPlus =>
        DifficultyColors.makeColorHtml(
          DifficultyColors.DIFFICULTY_EXPERT_COLOR,
          PluginBundle.message(s"luogu.difficulty.${this.productPrefix}")
        )
    }
  }
}

object LuoGuDifficulty {
  implicit val circeEncoder: Encoder[LuoGuDifficulty] = Encoder.encodeInt.contramap(_.value)
  implicit val circeDecoder: Decoder[LuoGuDifficulty] = Decoder.decodeInt.emap {
    case 0 => Right(LuoGuDifficulty.NonRated)
    case 1 => Right(LuoGuDifficulty.Beginner)
    case 2 => Right(LuoGuDifficulty.BasicMinus)
    case 3 => Right(LuoGuDifficulty.Basic)
    case 4 => Right(LuoGuDifficulty.BasicPlus)
    case 5 => Right(LuoGuDifficulty.Advanced)
    case 6 => Right(LuoGuDifficulty.AdvancedPlus)
    case 7 => Right(LuoGuDifficulty.NOIMinus)
    case 8 => Right(LuoGuDifficulty.NOIPlus)
    case _ => Left("Invalid difficulty value")
  }
}
