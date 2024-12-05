package com.wenjunhuang.codeepiphany.controllers.sidebar.jcef

import com.intellij.openapi.editor.colors.{ EditorColorsManager, EditorColorsScheme }
import com.intellij.util.ui.{ JBUI, UIUtil }

import java.awt.Color
import DescriptionStyleProvider.*
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions
import com.intellij.ui.JBColor

trait DescriptionStyleProvider {

  /** The padding of the body element in the description view with css padding property order: top, right, bottom, left
    */
  def bodyPadding: Option[(Int, Int, Int, Int)]

  def panelBackground: Color           = UIUtil.getPanelBackground
  def colorsScheme: EditorColorsScheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme
  def contrastedForeground: Color      = colorsScheme.getDefaultForeground.contrast(1.2)
  def textColor: Color                 = colorsScheme.getAttributes(HighlighterColors.TEXT).getForegroundColor
  def fontName: String                 = colorsScheme.getEditorFontName
  def fontSize: Int                    = AppEditorFontOptions.getInstance().getState.FONT_SIZE
  def lineHeight: Double               = AppEditorFontOptions.getInstance().getState.LINE_SPACING
  def scale: Float                     = UISettings.getInstance().getCurrentIdeScale
  def backgroundColor: Color           = colorsScheme.getDefaultBackground
  def foregroundColor: Color           = colorsScheme.getDefaultForeground
  def linkActiveForegroundColor: Color = JBUI.CurrentTheme.Link.Foreground.ENABLED
  def separatorColor: JBColor          = JBColor.namedColor("Group.separatorColor", panelBackground)
  def infoForeground: JBColor          = JBColor.namedColor("Component.infoForeground", contrastedForeground)
  def fenceBackgroundColor             = JBColor(Color(212, 222, 231, 255 / 4), Color(212, 222, 231, 25))
}
object DescriptionStyleProvider {
  extension (color: Color) {
    def contrast(coefficient: Double): Color =
      Color(
        (coefficient * (color.getRed - 128) + 128).toInt,
        (coefficient * (color.getGreen - 128) + 128).toInt,
        (coefficient * (color.getBlue - 128) + 128).toInt
      )

    def webRgba(alpha: Double = color.getAlpha.toDouble): String =
      s"rgba(${color.getRed}, ${color.getGreen}, ${color.getBlue}, ${alpha})"
  }
}
