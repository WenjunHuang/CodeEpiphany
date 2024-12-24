package com.wenjunhuang.codeepiphany.model.template;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%
%{
    public ChallengeFileTemplateTextLexer() {
      this((java.io.Reader)null);
    }
%}

%unicode
%class ChallengeFileTemplateTextLexer
%implements FlexLexer
%function advance
%type IElementType

ALPHA=[A-Za-z_]
DIGIT=[0-9]
MACRO="$"({ALPHA}|{DIGIT})+|"$""{"({ALPHA}|{DIGIT})+"}"|"$"({ALPHA}|{DIGIT})+"$"
DIRECTIVE="#"{ALPHA}+

%%

<YYINITIAL> "\\#" { return ChallengeFileTemplateTokenType.ESCAPE; }
<YYINITIAL> "\\$" { return ChallengeFileTemplateTokenType.ESCAPE; }
<YYINITIAL> "#[[" { return ChallengeFileTemplateTokenType.ESCAPE; }
<YYINITIAL> "]]#" { return ChallengeFileTemplateTokenType.ESCAPE; }
<YYINITIAL> {MACRO} {return ChallengeFileTemplateTokenType.MACRO; }
<YYINITIAL> {DIRECTIVE} { return ChallengeFileTemplateTokenType.DIRECTIVE;}
<YYINITIAL> [^] { return ChallengeFileTemplateTokenType.TEXT; }