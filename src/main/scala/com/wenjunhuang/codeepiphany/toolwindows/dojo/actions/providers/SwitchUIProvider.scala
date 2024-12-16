package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers

enum DojoUI {
  case Unauthenticated
  case QueryParameters
  case SearchByKeyword
}
trait SwitchUIProvider {
  def switchTo(ui: DojoUI): Unit
  def getCurrentUI: DojoUI
}
