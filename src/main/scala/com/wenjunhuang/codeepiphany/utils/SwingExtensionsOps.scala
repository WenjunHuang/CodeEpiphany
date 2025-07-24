package com.wenjunhuang.codeepiphany.utils

import com.intellij.util.MathUtil

import java.awt.Color

trait SwingExtensionsOps {
  extension (color: Color) {

    /** Simple linear contrast function.
      *
      * 0 < coefficient < 1 results in reduced contrast. coefficient > 1 results in increased contrast.
      */
    def contrast(coefficient: Double): Color =
      Color(
        MathUtil.clamp(coefficient * (color.getRed - 128) + 128, 0, 255).toInt,
        MathUtil.clamp(coefficient * (color.getGreen - 128) + 128, 0, 255).toInt,
        MathUtil.clamp(coefficient * (color.getBlue - 128) + 128, 0, 255).toInt,
        color.getAlpha
      )

    def webRgba(alpha: Double = color.getAlpha.toDouble / 255.0): String =
      s"rgba(${color.getRed}, ${color.getGreen}, ${color.getBlue}, ${alpha})"
  }
}
