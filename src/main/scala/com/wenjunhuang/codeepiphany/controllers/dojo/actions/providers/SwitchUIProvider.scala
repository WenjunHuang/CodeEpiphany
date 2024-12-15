package com.wenjunhuang.codeepiphany.controllers.dojo.actions.providers

trait SwitchUIProvider {
  def switchToQueryParamUI(): Unit
  def switchToKeywordUI(): Unit
}
