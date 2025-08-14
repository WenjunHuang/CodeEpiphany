package com.wenjunhuang.codeepiphany.services

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions
import com.intellij.openapi.editor.colors.{EditorColorsManager, EditorColorsScheme}
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import com.intellij.util.ui.{JBUI, UIUtil}
import com.wenjunhuang.codeepiphany.utils.extensions.*

import java.awt.Color

trait WebViewStyleProvider {

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
  def scale: Float = {
    val settings = UISettings.getInstance()
    if settings.getPresentationMode then settings.getPresentationModeIdeScale
    else settings.getIdeScale
  }

  def isDarkMode: Boolean = {
    val bgColor = colorsScheme.getDefaultBackground
    // Calculate relative luminance using the formula: 0.299R + 0.587G + 0.114B
    val luminance = (0.299 * bgColor.getRed + 0.587 * bgColor.getGreen + 0.114 * bgColor.getBlue) / 255
    luminance < 0.5 // If luminance is less than 0.5, consider it dark mode
  }

  def backgroundColor: Color           = colorsScheme.getDefaultBackground
  def foregroundColor: Color           = colorsScheme.getDefaultForeground
  def linkActiveForegroundColor: Color = JBUI.CurrentTheme.Link.Foreground.ENABLED
  def separatorColor: JBColor          = JBColor.namedColor("Group.separatorColor", panelBackground)
  def infoForeground: JBColor          = JBColor.namedColor("Component.infoForeground", contrastedForeground)
  def fenceBackgroundColor             = JBColor(Color(212, 222, 231, 255 / 4), Color(212, 222, 231, 25))

  def baseStyle: String = {
    val padding = bodyPadding.map { case (top, right, bottom, left) =>
      s"${top}px ${right}px ${bottom}px ${left}px"
    }.getOrElse("0")
    s"""
       |:root{
       |   --darkMode: ${if (isDarkMode) 1 else 0};
       |}
       |body {
       |    line-height: ${lineHeight};
       |    min-height: 100%;
       |    position: relative;
       |    background-color: ${backgroundColor.webRgba()};
       |    font-family :${fontName},-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji;
       |    font-size: ${fontSize}px;
       |    padding: $padding;
       |    color: ${foregroundColor.webRgba()};
       |}
       |
       |a {
       |  color: ${linkActiveForegroundColor.webRgba()}
       |}
       |
       |table td, table th {
       |    border: 1px solid ${separatorColor.webRgba()};
       |}
       |
       |hr {
       |    background-color: ${separatorColor.webRgba()};
       |}
       |
       |kbd, tr {
       |    border: 1px solid ${separatorColor.webRgba()};
       |}
       |
       |blockquote {
       |    border-left: 2px solid ${linkActiveForegroundColor.webRgba(0.4)}
       |}
       |
       |blockquote, code, pre {
       |    overflow: auto;
       |    background-color: ${panelBackground.webRgba()}
       |}
       |
       |${JBCefScrollbarsHelper.getOverlayScrollbarStyle}
     """.stripMargin
  }
}

object WebViewStyleProvider {
  def apply(body: (Int, Int, Int, Int)): WebViewStyleProvider = new WebViewStyleProvider {
    override def bodyPadding: Option[(Int, Int, Int, Int)] = Some(body)
  }

  val DEFAULT: WebViewStyleProvider = new WebViewStyleProvider {
    override def bodyPadding: Option[(Int, Int, Int, Int)] = None
  }
}
