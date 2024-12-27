package com.wenjunhuang.codeepiphany.model.template.lexer

import com.intellij.lexer.{FlexAdapter, MergingLexerAdapter}
import com.intellij.psi.tree.TokenSet

class ChallengeFileTemplateTextLexer
    extends MergingLexerAdapter(
      new FlexAdapter(new _ChallengeFileTemplateTextLexer() {
        override def reset(buffer: CharSequence, start: Int, end: Int, initialState: Int): Unit = {
          super.reset(buffer, start, end, initialState)
          onReset()
        }
      }),
      TokenSet.create(ChallengeFileTemplateTokenType.TEXT)
    ) {}
