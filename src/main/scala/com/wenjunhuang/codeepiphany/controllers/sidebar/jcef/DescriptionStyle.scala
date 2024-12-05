package com.wenjunhuang.codeepiphany.controllers.sidebar.jcef

import com.intellij.ide.ui.UIThemeKt
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.JBUI.CurrentTheme.Toolbar

import java.awt.{ Color, Insets }

object DescriptionStyle {
  def getStyle(styleProvider: DescriptionStyleProvider): String = {
    val colorsScheme = EditorColorsManager.getInstance().getSchemeForCurrentUITheme
    val textColor    = colorsScheme.getAttributes(HighlighterColors.TEXT).getForegroundColor
    val padding = styleProvider.bodyPadding.map { case (top, right, bottom, left) =>
      s"${top}px ${right}px ${bottom}px ${left}px"
    }.getOrElse("0")

    s"""
       |body {
       |    margin: 0;
       |    color: ${colorString(textColor)};
       |    background-color: ${colorString(colorsScheme.getDefaultBackground)};
       |    font-family :${colorsScheme.getEditorFontName};
       |    padding: ${padding}
       |}
       |
       |#container {
       |    transition: all 0.2s ease-in;
       |}
       |""".stripMargin
  }

  private def colorString(color: Color): String =
    s"rgba(${color.getRed}, ${color.getGreen}, ${color.getBlue}, ${color.getAlpha / 255.0})"

}
