package com.wenjunhuang.codeepiphany.model.template

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
  val MACRO = IElementType("MACRO", Language.ANY)

  @static
  val DIRECTIVE = IElementType("DIRECTIVE", Language.ANY)

}
