package com.wenjunhuang.codeepiphany.utils

import java.awt.Color

import com.intellij.ui.{ Gray, JBColor }

object ColorUtils {
  val ERROR_FOREGROUND = JBColor(0xeb353a, 0xee615e)
  val ERROR_BACKGROUND = JBColor(0xfdeff0, 0x362b2a)

  val LABEL_GRAY_COLOR: JBColor = JBColor.namedColor("Label.infoForeground", new JBColor(Gray._120, Gray._135))

  def hexToColor(hex: String): Option[Color] = {
    if hex.startsWith("#") then
      try
        val colors = hex.substring(1).grouped(2).map(Integer.parseInt(_, 16)).toList
        if (colors.length == 3) {
          Some(new Color(colors.head, colors(1), colors(2)))
        } else if (colors.length == 4) {
          Some(new Color(colors.head, colors(1), colors(2), colors(3)))
        } else {
          None
        }
      catch
        e => None
    else None
  }

//  def adjustBrightness(color: String, percent: Double): String = {

//    val (r, g, b) = hexToRgb(color)
//    val factor = 1 + percent / 100
//    val newR = Math.min(255, r * factor).toInt
//    val newG = Math.min(255, g * factor).toInt
//    val newB = Math.min(255, b * factor).toInt
//    rgbToHex(newR, newG, newB)
//  }

}
