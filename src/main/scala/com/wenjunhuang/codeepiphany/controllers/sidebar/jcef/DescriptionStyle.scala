package com.wenjunhuang.codeepiphany.controllers.sidebar.jcef

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions
import com.intellij.ui.JBColor
import com.intellij.util.ui.{ JBUI, UIUtil }

import java.awt.Color

object DescriptionStyle {
  def getDefaultStyle(styleProvider: DescriptionStyleProvider): String = {
    val panelBackground           = UIUtil.getPanelBackground
    val colorsScheme              = EditorColorsManager.getInstance().getSchemeForCurrentUITheme
    val contrastedForeground      = colorsScheme.getDefaultForeground.contrast(1.2)
    val textColor                 = colorsScheme.getAttributes(HighlighterColors.TEXT).getForegroundColor
    val fontSize                  = AppEditorFontOptions.getInstance().getState.FONT_SIZE
    val scale                     = UISettings.getInstance().getCurrentIdeScale
    val backgroundColor           = colorsScheme.getDefaultBackground.webRgba()
    val foregroundColor           = colorsScheme.getDefaultForeground.webRgba()
    val linkActiveForegroundColor = JBUI.CurrentTheme.Link.Foreground.ENABLED
    val separatorColor            = JBColor.namedColor("Group.separatorColor", panelBackground).webRgba()
    val infoForeground            = JBColor.namedColor("Component.infoForeground", contrastedForeground).webRgba()
    val fenceBackgroundColor      = JBColor(Color(212, 222, 231, 255 / 4), Color(212, 222, 231, 25))

    val padding = styleProvider.bodyPadding.map { case (top, right, bottom, left) =>
      s"${top}px ${right}px ${bottom}px ${left}px"
    }.getOrElse("0")

    // language=CSS
    s"""
       |:root {
       | $FONT_SIZE: ${fontSize}px;
       |}
       |* {
       | border: 0 solid;
       | box-sizing: border-box;
       |}
       |
       |body {
       |    line-height: 1.5;
       |    min-height: 100%;
       |    position: relative;
       |    background-color: ${backgroundColor};
       |    font-family :${colorsScheme.getEditorFontName},-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji;
       |    font-size: var(${FONT_SIZE}) !important;
       |    padding: $padding;
       |}
       |
       |body, p, blockquote, ul, ol, dl, table, pre, code, tr {
       |    color: ${foregroundColor};
       |}
       |
       |menu, ol, ul {
       |  list-style: none;
       |}
       |
       |fieldset, menu, ol, ul {
       |margin: 0;
       |padding: 0;
       |}
       |
       |a {
       |  color: ${linkActiveForegroundColor.webRgba()}
       |}
       |
       |table td, table th {
       |    border: 1px solid $separatorColor;
       |}
       |
       |hr {
       |    background-color: $separatorColor;
       |}
       |
       |kbd, tr {
       |    border: 1px solid $separatorColor;
       |}
       |
       |h6 {
       |  color: $infoForeground;
       |}
       |
       |blockquote {
       |    border-left: 2px solid ${linkActiveForegroundColor.webRgba(0.4)}
       |}
       |
       |blockquote, code, pre {
       |    background-color: ${fenceBackgroundColor.webRgba(fenceBackgroundColor.getAlpha / 255.0)}
       |}
       |
       |""".stripMargin
  }

  def getLeetcodeCNStyle(styleProvider: DescriptionStyleProvider): String =
    // language=CSS
    s"""
       |#container pre {
       |    color: #fff9;
       |    background-color: unset;
       |    border-left: 2px solid #fff9;
       |    margin-bottom: 1rem;
       |    margin-top: 1rem;
       |    padding-left: 1rem;
       |}
       |#container p {
       |  margin-bottom: 1rem;
       |}
       |
       |#container ul {
       |    list-style-type: disc;
       |    margin-bottom: 1rem;
       |    margin-left: 1rem;
       |    margin-right: 1rem;
       |}
       |
       |#container ul > li {
       |    margin-bottom: .75rem;
       |}
       |""".stripMargin

  extension (color: Color) {
    private def contrast(coefficient: Double): Color =
      Color(
        (coefficient * (color.getRed - 128) + 128).toInt,
        (coefficient * (color.getGreen - 128) + 128).toInt,
        (coefficient * (color.getBlue - 128) + 128).toInt
      )

    private def webRgba(alpha: Double = color.getAlpha.toDouble): String =
      s"rgba(${color.getRed}, ${color.getGreen}, ${color.getBlue}, ${alpha})"
  }

  private val FONT_SIZE = "--default-font-size"
  private val SCALE     = "--scale"
}
