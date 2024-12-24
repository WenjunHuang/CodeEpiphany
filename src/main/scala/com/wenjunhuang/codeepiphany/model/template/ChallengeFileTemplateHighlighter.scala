package com.wenjunhuang.codeepiphany.model.template

import com.intellij.codeInsight.template.impl.TemplateColors
import com.intellij.lexer.{ FlexAdapter, Lexer, MergingLexerAdapter }
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.{ IElementType, TokenSet }
import com.wenjunhuang.codeepiphany.model.template.ChallengeFileTemplateHighlighter.createDefaultLexer

class ChallengeFileTemplateHighlighter extends SyntaxHighlighterBase {
  private val myLexer = createDefaultLexer()

  override def getHighlightingLexer: Lexer = myLexer

  override def getTokenHighlights(tokenType: IElementType): Array[TextAttributesKey] =
    tokenType match {
      case ChallengeFileTemplateTokenType.MACRO | ChallengeFileTemplateTokenType.DIRECTIVE =>
        SyntaxHighlighterBase.pack(TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES)
      case _ => Array.empty
    }
}

object ChallengeFileTemplateHighlighter {
  private def createDefaultLexer(): Lexer = MergingLexerAdapter(
    FlexAdapter(ChallengeFileTemplateTextLexer()),
    TokenSet.create(ChallengeFileTemplateTokenType.TEXT)
  )

}
