package com.wenjunhuang.codeepiphany.model.template

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.{EditorColorsManager, TextAttributesKey}
import com.intellij.openapi.editor.ex.util.{LayerDescriptor, LayeredLexerEditorHighlighter, LexerEditorHighlighter}
import com.intellij.openapi.editor.highlighter.{EditorHighlighter, EditorHighlighterFactory}
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import com.wenjunhuang.codeepiphany.model.Language
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

object ChallengeFileTemplateHighlighter {
  def createVelocityTemplateLanguageEditorHighlighter(
    project: Project,
    language: Option[Language]
  ): EditorHighlighter = {
    val velocityFiletype = FileTypeManager.getInstance.getFileTypeByExtension("ft")
    if velocityFiletype != FileTypes.UNKNOWN then
      EditorHighlighterFactory
        .getInstance()
        .createEditorHighlighter(
          project,
          LightVirtualFile(
            s"template.${language.map(_.fileExt).getOrElse(FileTypes.PLAIN_TEXT.getDefaultExtension)}.ft"
          )
        )
    else
      val syntaxHighlighter = createLanguageSyntaxHighlighter(project, language)
      val editorHighlighter = LayeredLexerEditorHighlighter(
        ChallengeFileTemplateHighlighter(),
        EditorColorsManager.getInstance().getGlobalScheme
      )
      editorHighlighter.registerLayer(
        ChallengeFileTemplateTokenType.TEXT,
        LayerDescriptor(syntaxHighlighter, "")
      )

      editorHighlighter
  }

  def createVelocityTemplatePlainTextHighlighter(project: Project): EditorHighlighter = {
    val velocityFiletype = FileTypeManager.getInstance.getFileTypeByExtension("ft")
    if velocityFiletype != FileTypes.UNKNOWN then
      EditorHighlighterFactory
        .getInstance()
        .createEditorHighlighter(
          project,
          LightVirtualFile(s"template.${FileTypes.PLAIN_TEXT.getDefaultExtension}.ft")
        )
    else
      val syntaxHighlighter =
        SyntaxHighlighterFactory.getSyntaxHighlighter(FileTypes.PLAIN_TEXT, project, null)
      val editorHighlighter = LayeredLexerEditorHighlighter(
        ChallengeFileTemplateHighlighter(),
        EditorColorsManager.getInstance().getGlobalScheme
      )
      editorHighlighter.registerLayer(
        ChallengeFileTemplateTokenType.TEXT,
        LayerDescriptor(syntaxHighlighter, "")
      )

      editorHighlighter

  }

  def createLanguageEditorHighlighter(
    project: Project,
    language: Option[Language]
  ): EditorHighlighter = {
    val syntaxHighlighter = createLanguageSyntaxHighlighter(project, language)
    LexerEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().getGlobalScheme)
  }

  def createLanguageSyntaxHighlighter(
    project: Project,
    language: Option[Language]
  ): SyntaxHighlighter = {
    val fileType = language match
      case None => FileTypes.PLAIN_TEXT
      case Some(lang) =>
        Option(FileTypeManager.getInstance().getFileTypeByExtension(lang.fileExt)).map { fileType =>
          if fileType == FileTypes.UNKNOWN then FileTypes.PLAIN_TEXT else fileType
        }.getOrElse(FileTypes.PLAIN_TEXT)

    Option(SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, project, null))
      .getOrElse(PlainSyntaxHighlighter())
  }
}
