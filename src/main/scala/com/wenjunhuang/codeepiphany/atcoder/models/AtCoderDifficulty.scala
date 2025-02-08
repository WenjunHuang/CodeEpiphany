package com.wenjunhuang.codeepiphany.atcoder.models

import java.awt.Color

import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderDifficulty.*
import com.wenjunhuang.codeepiphany.utils.ColorUtils

enum AtCoderDifficulty {
  case Grey
  case Brown
  case Green
  case Cyan
  case Blue
  case Yellow
  case Orange
  case Red
  case Bronze
  case Silver
  case Gold

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

  def fromInt(difficulty: Int): AtCoderDifficulty = {
    if difficulty < 400 then Grey
    else if difficulty < 800 then Brown
    else if difficulty < 1200 then Green
    else if difficulty < 1600 then Cyan
    else if difficulty < 2000 then Blue
    else if difficulty < 2400 then Yellow
    else if difficulty < 2800 then Orange
    else if difficulty < 3200 then Red
    else if difficulty < 3600 then Bronze
    else if difficulty < 4000 then Silver
    else Gold
  }

  def atCoderDifficultyRange(difficulty: AtCoderDifficulty): (Int, Int) = {
    difficulty match {
      case Grey   => (0, 399)
      case Brown  => (400, 799)
      case Green  => (800, 1199)
      case Cyan   => (1200, 1599)
      case Blue   => (1600, 1999)
      case Yellow => (2000, 2399)
      case Orange => (2400, 2799)
      case Red    => (2800, 3199)
      case Bronze => (3200, 3599)
      case Silver => (3600, 3999)
      case Gold   => (4000, Int.MaxValue)
    }
  }

  def prettyPrint(difficulty: AtCoderDifficulty): String = {
    difficulty match {
      case Grey   => "[0, 399]"
      case Brown  => "[400, 799]"
      case Green  => "[800, 1199]"
      case Cyan   => "[1200, 1599]"
      case Blue   => "[1600, 1999]"
      case Yellow => "[2000, 2399]"
      case Orange => "[2400, 2799]"
      case Red    => "[2800, 3199]"
      case Bronze => "[3200, 3599]"
      case Silver => "[3600, 3999]"
      case Gold   => "[>=4000]"
    }

  }

  def calculateDisplayDifficulty(irtDifficulty: Int): Int =
    if irtDifficulty >= 400.0 then irtDifficulty
    else (400.0 / math.exp(1.0 - irtDifficulty / 400.0)).toInt
}
