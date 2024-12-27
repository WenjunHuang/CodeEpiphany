package com.wenjunhuang.codeepiphany.model.template

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.wenjunhuang.codeepiphany.model.template.lexer.{ChallengeFileTemplateTextLexer, ChallengeFileTemplateTokenType}

class ChallengeFileTemplateHighlighter extends SyntaxHighlighterBase {
  private val myLexer = ChallengeFileTemplateTextLexer()

  override def getHighlightingLexer: Lexer = myLexer

  override def getTokenHighlights(tokenType: IElementType): Array[TextAttributesKey] =
    tokenType match {
      case ChallengeFileTemplateTokenType.START_REF |
          ChallengeFileTemplateTokenType.START_REF_FORMAL |
          ChallengeFileTemplateTokenType.START_REF_SILENT |
          ChallengeFileTemplateTokenType.START_REF_SILENT_FORMAL |
          ChallengeFileTemplateTokenType.END_REF_FORMAL =>
        SyntaxHighlighterBase.pack(DefaultLanguageHighlighterColors.KEYWORD)
      case ChallengeFileTemplateTokenType.DIRECTIVE =>
        SyntaxHighlighterBase.pack(DefaultLanguageHighlighterColors.KEYWORD)
      case ChallengeFileTemplateTokenType.OPERATOR =>
        SyntaxHighlighterBase.pack(DefaultLanguageHighlighterColors.OPERATION_SIGN)
      case ChallengeFileTemplateTokenType.START_PAREN | ChallengeFileTemplateTokenType.END_PAREN =>
        SyntaxHighlighterBase.pack(DefaultLanguageHighlighterColors.PARENTHESES)
      case ChallengeFileTemplateTokenType.IDENTIFIER =>
        // identifier color
        SyntaxHighlighterBase.pack(DefaultLanguageHighlighterColors.STATIC_FIELD)
      case _ => Array.empty
    }
}
