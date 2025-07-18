package com.wenjunhuang.codeepiphany.atcoder.models

import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderDifficulty.*
import com.wenjunhuang.codeepiphany.utils.ColorUtils
import io.circe.{Decoder, Encoder}
import org.typelevel.ci.CIString

import java.awt.Color

enum AtCoderDifficulty(val lowerBound: Int, val upperBound: Int) {
  case Grey   extends AtCoderDifficulty(Int.MinValue, 399)
  case Brown  extends AtCoderDifficulty(400, 799)
  case Green  extends AtCoderDifficulty(800, 1199)
  case Cyan   extends AtCoderDifficulty(1200, 1599)
  case Blue   extends AtCoderDifficulty(1600, 1999)
  case Yellow extends AtCoderDifficulty(2000, 2399)
  case Orange extends AtCoderDifficulty(2400, 2799)
  case Red    extends AtCoderDifficulty(2800, 3199)
  case Bronze extends AtCoderDifficulty(3200, 3599)
  case Silver extends AtCoderDifficulty(3600, 3999)
  case Gold   extends AtCoderDifficulty(4000, Int.MaxValue)

  def showAsHtml: String =
    this match
      case Grey =>
        s"<html><font color='${GREY_COLOR}'>${prettyPrint(this)}</font></html>"
      case Brown =>
        s"<html><font color='${BROWN_COLOR}'>${prettyPrint(this)}</font></html>"
      case Green =>
        s"<html><font color='${GREEN_COLOR}'>${prettyPrint(this)}</font></html>"
      case Cyan =>
        s"<html><font color='${CYAN_COLOR}'>${prettyPrint(this)}</font></html>"
      case Blue =>
        s"<html><font color='${BLUE_COLOR}'>${prettyPrint(this)}</font></html>"
      case Yellow =>
        s"<html><font color='${YELLOW_COLOR}'>${prettyPrint(this)}</font></html>"
      case Orange =>
        s"<html><font color='${ORANGE_COLOR}'>${prettyPrint(this)}</font></html>"
      case Red =>
        s"<html><font color='${RED_COLOR}'>${prettyPrint(this)}</font></html>"
      case Bronze =>
        s"<html><font color='${BRONZE_COLOR}'>${prettyPrint(this)}</font></html>"
      case Silver =>
        s"<html><font color='${SILVER_COLOR}'>${prettyPrint(this)}</font></html>"
      case Gold =>
        s"<html><font color='${GOLD_COLOR}'>${prettyPrint(this)}</font></html>"

  def showAsHtml(value: Int): String =
    this match
      case Grey =>
        s"<html><font color='${GREY_COLOR}'>${value}</font></html>"
      case Brown =>
        s"<html><font color='${BROWN_COLOR}'>${value}</font></html>"
      case Green =>
        s"<html><font color='${GREEN_COLOR}'>${value}</font></html>"
      case Cyan =>
        s"<html><font color='${CYAN_COLOR}'>${value}</font></html>"
      case Blue =>
        s"<html><font color='${BLUE_COLOR}'>${value}</font></html>"
      case Yellow =>
        s"<html><font color='${YELLOW_COLOR}'>${value}</font></html>"
      case Orange =>
        s"<html><font color='${ORANGE_COLOR}'>${value}</font></html>"
      case Red =>
        s"<html><font color='${RED_COLOR}'>${value}</font></html>"
      case Bronze =>
        s"<html><font color='${BRONZE_COLOR}'>${value}</font></html>"
      case Silver =>
        s"<html><font color='${SILVER_COLOR}'>${value}</font></html>"
      case Gold =>
        s"<html><font color='${GOLD_COLOR}'>${value}</font></html>"
  def color: Color =
    this match
      case Grey   => ColorUtils.hexToColor(GREY_COLOR).getOrElse(Color.GRAY)
      case Brown  => ColorUtils.hexToColor(BROWN_COLOR).getOrElse(Color.GRAY)
      case Green  => ColorUtils.hexToColor(GREEN_COLOR).getOrElse(Color.GRAY)
      case Cyan   => ColorUtils.hexToColor(CYAN_COLOR).getOrElse(Color.GRAY)
      case Blue   => ColorUtils.hexToColor(BLUE_COLOR).getOrElse(Color.GRAY)
      case Yellow => ColorUtils.hexToColor(YELLOW_COLOR).getOrElse(Color.GRAY)
      case Orange => ColorUtils.hexToColor(ORANGE_COLOR).getOrElse(Color.GRAY)
      case Red    => ColorUtils.hexToColor(RED_COLOR).getOrElse(Color.GRAY)
      case Bronze => ColorUtils.hexToColor(BRONZE_COLOR).getOrElse(Color.GRAY)
      case Silver => ColorUtils.hexToColor(SILVER_COLOR).getOrElse(Color.GRAY)
      case Gold   => ColorUtils.hexToColor(GOLD_COLOR).getOrElse(Color.GRAY)
}

object AtCoderDifficulty {
  // 核心难度色系（适配暗色主题）
  val GREY_COLOR   = "#808080" // 中灰 - 入门级（比默认灰更醒目）
  val BROWN_COLOR  = "#CD853F" // 古铜棕 - 基础级（带橙调）
  val GREEN_COLOR  = "#20C997" // 薄荷绿 - 中级（高对比冷色）
  val CYAN_COLOR   = "#17A2B8" // 亮青 - 进阶（科技感）
  val BLUE_COLOR   = "#007BFF" // 深天蓝 - 困难（保留品牌色）
  val YELLOW_COLOR = "#FFC107" // 琥珀黄 - 挑战级（暗背景下醒目）
  val ORANGE_COLOR = "#FD7E14" // 橙红 - 专家级（高警示性）
  val RED_COLOR    = "#DC3545" // 警示红 - 大师级（最高视觉权重）

  // 特殊成就色系（比赛奖项）
  val BRONZE_COLOR = "#CD7F32" // 古铜金 - 铜牌题
  val SILVER_COLOR = "#C0C0C0" // 银灰 - 银牌题（带金属光泽）
  val GOLD_COLOR   = "#FFD700" // 黄金 - 金牌题（高饱和度）

  def fromCIString(value: CIString): Option[AtCoderDifficulty] =
    if value == CIString(Grey.toString) then Some(Grey)
    else if value == CIString(Brown.toString) then Some(Brown)
    else if value == CIString(Green.toString) then Some(Green)
    else if value == CIString(Cyan.toString) then Some(Cyan)
    else if value == CIString(Blue.toString) then Some(Blue)
    else if value == CIString(Yellow.toString) then Some(Yellow)
    else if value == CIString(Orange.toString) then Some(Orange)
    else if value == CIString(Red.toString) then Some(Red)
    else if value == CIString(Bronze.toString) then Some(Bronze)
    else if value == CIString(Silver.toString) then Some(Silver)
    else if value == CIString(Gold.toString) then Some(Gold)
    else None

  def fromString(difficulty: String): Option[AtCoderDifficulty] = {
    difficulty.toIntOption.map(fromInt)
  }

  def fromInt(difficulty: Int): AtCoderDifficulty = {
    if difficulty <= Grey.upperBound then Grey
    else if difficulty <= Brown.upperBound then Brown
    else if difficulty <= Green.upperBound then Green
    else if difficulty <= Cyan.upperBound then Cyan
    else if difficulty <= Blue.upperBound then Blue
    else if difficulty <= Yellow.upperBound then Yellow
    else if difficulty <= Orange.upperBound then Orange
    else if difficulty <= Red.upperBound then Red
    else if difficulty <= Bronze.upperBound then Bronze
    else if difficulty <= Silver.upperBound then Silver
    else Gold
  }

  def showAsHtmlFromStorage(difficulty: String): String = {
    fromString(difficulty).map(_.showAsHtml).getOrElse("")
  }

  def atCoderDifficultyRange(difficulty: AtCoderDifficulty): (Int, Int) = {
    (difficulty.lowerBound, difficulty.upperBound)
  }

  def prettyPrint(difficulty: AtCoderDifficulty): String = {
    difficulty match {
      case Grey => s"[~-${Grey.upperBound}]"
      case Gold => s"[${Gold.lowerBound}-~]"
      case _    => s"[${difficulty.lowerBound}-${difficulty.upperBound}]"
    }
  }

  /** Calculate the display difficulty based on the IRT difficulty(from kenkoooo's source code)
    */
  def calculateDisplayDifficulty(irtDifficulty: Int): Int =
    if irtDifficulty >= 400.0 then irtDifficulty
    else (400.0 / math.exp(1.0 - irtDifficulty / 400.0)).toInt

  implicit val circeEncoder: Encoder[AtCoderDifficulty] = Encoder.encodeString.contramap[AtCoderDifficulty](_.toString)
  implicit val circeDecoder: Decoder[AtCoderDifficulty] =
    Decoder.decodeString.emap(v => fromCIString(CIString(v)).toRight("Unknown difficulty value"))
}
