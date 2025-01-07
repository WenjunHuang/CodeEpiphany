package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers

import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}

trait ChallengeProvider {
  def openCurrentSelectedChallenge(language:Language,languageVersion:LanguageVersion): Unit
  
  def getLanguages: List[(Language,LanguageVersion)]
}
