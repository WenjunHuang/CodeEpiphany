package com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.actions

import icons.CodeEpiphanyIcons

import com.intellij.openapi.actionSystem.*

import com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.HackerRankUI
import com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.HackerRankUI.*
import com.wenjunhuang.codeepiphany.toolwindows.dojo.hackerrank.actions.SwitchUIAction.*

class SwitchUIAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(SWITCHUI_PROVIDER_KEY.getData(e.getDataContext)) match
      case Some(provider) =>
        provider.getCurrentUI match
          case QueryParameters => provider.switchTo(SearchByKeyword)
          case SearchByKeyword => provider.switchTo(QueryParameters)
          case _               => ()
      case None => ()

  private def updateIcon(ui: HackerRankUI, present: Presentation): Unit =
    ui match
      case Unauthenticated => ()
      case QueryParameters => present.setIcon(CodeEpiphanyIcons.SEARCH)
      case SearchByKeyword => present.setIcon(CodeEpiphanyIcons.QUERY_PARAM)

  override def update(e: AnActionEvent): Unit =
    Option(SWITCHUI_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => e.getPresentation.setEnabledAndVisible(false)
      case Some(provider) =>
        if provider.getCurrentUI == Unauthenticated then e.getPresentation.setEnabledAndVisible(false)
        else
          e.getPresentation.setEnabledAndVisible(true)
          updateIcon(provider.getCurrentUI, e.getPresentation)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object SwitchUIAction {
  val SWITCHUI_PROVIDER_KEY = DataKey.create[SwitchUIProvider]("SWITCHUI_PROVIDER_KEY")

  trait SwitchUIProvider {
    def switchTo(ui: HackerRankUI): Unit

    def getCurrentUI: HackerRankUI
  }
}
