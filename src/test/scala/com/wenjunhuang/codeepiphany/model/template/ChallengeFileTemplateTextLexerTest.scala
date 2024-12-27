package com.wenjunhuang.codeepiphany.model.template

import com.intellij.testFramework.LexerTestCase
import com.wenjunhuang.codeepiphany.model.template.lexer.ChallengeFileTemplateTextLexer

class ChallengeFileTemplateTextLexerTest extends LexerTestCase {
  override def createLexer() = new ChallengeFileTemplateTextLexer()

  override def getDirPath = "testResources/template/"

  def testSimple(): Unit =
    doTest(
      s"""
        |public $${velocity.camelCaseName($$challenge.slug)} {
        |  public static void main(String[] args) {
        |  }
        |}
        |""".stripMargin
    )
}
