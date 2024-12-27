package com.wenjunhuang.codeepiphany.model.template.lexer;

import com.intellij.psi.tree.IElementType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import com.intellij.lexer.FlexLexer;

%%
%{
    private IntArrayList stateStack = new IntArrayList();

    public _ChallengeFileTemplateTextLexer() {
      this((java.io.Reader)null);
      onReset();
    }

    private void pushState(int state){
        int currentState = yystate();
        assert currentState != YYINITIAL || stateStack.isEmpty() : "Can't push initial state into the not empty stack";
        stateStack.push(currentState);
        yybegin(state);
    }

    private void popState() {
        assert !stateStack.isEmpty() : "States stack is empty";
        yybegin(stateStack.popInt());
    }

    protected void onReset() {
      stateStack.clear();
    }
%}

%unicode
%class _ChallengeFileTemplateTextLexer
%implements FlexLexer
%function advance
%type IElementType


ALPHA=[A-Za-z_]
DIGIT=[0-9]
DOT=[\.]
START_REF="$"
START_REF_SILENT="$!"
START_REF_FORMAL="${"
START_REF_SILENT_FORMAL="$!{"
IDENTIFIER={ALPHA}({ALPHA}|{DIGIT}|{DOT})+
DIRECTIVE="#"{ALPHA}+
OPERATORS="+"|"-"|"|"|"/"|"*"
LineTerminator           = \r\n | \r | \n
WHITE_SPACE=[ \t\f] {LineTerminator}*

%state REFERENCE
%state REFERENCE_FORMAL
%state REFERENCE_METHOD
%%

<YYINITIAL> "\\#" { return ChallengeFileTemplateTokenType.ESCAPE; }
<YYINITIAL> "\\$" { return ChallengeFileTemplateTokenType.ESCAPE; }
<YYINITIAL> "#[[" { return ChallengeFileTemplateTokenType.ESCAPE; }
<YYINITIAL> "]]#" { return ChallengeFileTemplateTokenType.ESCAPE; }
<YYINITIAL> {START_REF_SILENT_FORMAL} { pushState(REFERENCE_FORMAL); return ChallengeFileTemplateTokenType.START_REF_SILENT_FORMAL; }
<YYINITIAL> {START_REF_FORMAL} { pushState(REFERENCE_FORMAL); return ChallengeFileTemplateTokenType.START_REF_FORMAL; }
<YYINITIAL> {START_REF_SILENT} { pushState(REFERENCE); return ChallengeFileTemplateTokenType.START_REF_SILENT; }
<YYINITIAL> {START_REF} { pushState(REFERENCE); return ChallengeFileTemplateTokenType.START_REF; }
<YYINITIAL> {DIRECTIVE} { return ChallengeFileTemplateTokenType.DIRECTIVE; }
<YYINITIAL> [^] { return ChallengeFileTemplateTokenType.TEXT; }

<REFERENCE_FORMAL> {
{IDENTIFIER} { return ChallengeFileTemplateTokenType.IDENTIFIER; }
      "(" { pushState(REFERENCE_METHOD);return ChallengeFileTemplateTokenType.START_PAREN; }
"}" { popState(); return ChallengeFileTemplateTokenType.END_REF_FORMAL; }
[^] { popState();yypushback(yylength()); }
}

<REFERENCE> {
{IDENTIFIER} { return ChallengeFileTemplateTokenType.IDENTIFIER; }
 "(" { pushState(REFERENCE_METHOD);return ChallengeFileTemplateTokenType.START_PAREN; }
[^] { popState(); yypushback(yylength()); }
}

<REFERENCE_METHOD> {
")" { popState(); return ChallengeFileTemplateTokenType.END_PAREN; }
{START_REF_SILENT_FORMAL} { pushState(REFERENCE_FORMAL); return ChallengeFileTemplateTokenType.START_REF_SILENT_FORMAL; }
{START_REF_FORMAL} { pushState(REFERENCE_FORMAL); return ChallengeFileTemplateTokenType.START_REF_FORMAL; }
{START_REF_SILENT} { pushState(REFERENCE); return ChallengeFileTemplateTokenType.START_REF_SILENT; }
{START_REF} { pushState(REFERENCE);return ChallengeFileTemplateTokenType.START_REF; }
{OPERATORS} {return ChallengeFileTemplateTokenType.OPERATOR;}
      [^] {popState();yypushback(yylength());}
}