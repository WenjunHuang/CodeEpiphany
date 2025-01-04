package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers

import com.wenjunhuang.codeepiphany.model.Language

trait ChallengeProvider {
  def openCurrentSelectedChallenge(language:Language): Unit
  
  def getLanguages: List[Language]
}
