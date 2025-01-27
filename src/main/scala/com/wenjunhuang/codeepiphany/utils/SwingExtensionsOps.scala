package com.wenjunhuang.codeepiphany.utils

import java.awt.{ Color, Graphics2D }
import scala.util.Using.Releasable

trait SwingExtensionsOps {
  extension (color: Color) {

    /** Simple linear contrast function.
      *
      * 0 < coefficient < 1 results in reduced contrast. coefficient > 1 results in increased contrast.
      */
    def contrast(coefficient: Double): Color =
      Color(
        Math.clamp((coefficient * (color.getRed - 128) + 128).toLong, 0, 255),
        Math.clamp((coefficient * (color.getGreen - 128) + 128).toInt, 0, 255),
        Math.clamp((coefficient * (color.getBlue - 128) + 128).toInt, 0, 255),
        color.getAlpha
      )

    def webRgba(alpha: Double = color.getAlpha.toDouble / 255.0): String =
      s"rgba(${color.getRed}, ${color.getGreen}, ${color.getBlue}, ${alpha})"
  }
}
