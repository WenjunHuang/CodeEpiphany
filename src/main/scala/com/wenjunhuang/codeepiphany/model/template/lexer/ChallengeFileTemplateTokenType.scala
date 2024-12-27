package com.wenjunhuang.codeepiphany.model.template.lexer

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType

import scala.annotation.static

class ChallengeFileTemplateTokenType {}
object ChallengeFileTemplateTokenType {
 
  @static
  val ESCAPE = IElementType("ESCAPE", Language.ANY)
  
  @static
  val TEXT = IElementType("TEXT", Language.ANY)

  @static
  val IDENTIFIER = IElementType("IDENTIFIER", Language.ANY)

  @static
  val START_PAREN = IElementType("START_PAREN", Language.ANY)
  @static
  val END_PAREN = IElementType("END_PAREN", Language.ANY)

  @static
  val START_REF = IElementType("START_REF",Language.ANY)

  @static
  val START_REF_SILENT = IElementType("START_REF_SILENT",Language.ANY)

  @static
  val START_REF_FORMAL= IElementType("START_REF_FORMAL",Language.ANY)

  @static
  val START_REF_SILENT_FORMAL= IElementType("START_REF_SILENT_FORMAL",Language.ANY)

  @static
  val END_REF_FORMAL = IElementType("END_REF_FORMAL",Language.ANY)

  @static
  val DIRECTIVE = IElementType("DIRECTIVE", Language.ANY)

  @static
  val OPERATOR = IElementType("OPERATOR",Language.ANY)

}
