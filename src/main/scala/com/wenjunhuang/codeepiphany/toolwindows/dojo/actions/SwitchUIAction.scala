package com.wenjunhuang.codeepiphany.toolwindows.dojo.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent, Presentation}
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.keys.SWITCHUI_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers.DojoUI
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers.DojoUI.*
import icons.CodeEpiphanyIcons

class SwitchUIAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(SWITCHUI_PROVIDER_KEY.getData(e.getDataContext)) match
      case Some(provider) =>
        provider.getCurrentUI match
          case QueryParameters => provider.switchTo(DojoUI.SearchByKeyword)
          case SearchByKeyword => provider.switchTo(DojoUI.QueryParameters)
          case _               => ()
      case None => ()

  private def updateIcon(ui: DojoUI, present: Presentation): Unit =
    ui match
      case Unauthenticated => ()
      case QueryParameters => present.setIcon(CodeEpiphanyIcons.DojoKeywordUIIcon)
      case SearchByKeyword => present.setIcon(CodeEpiphanyIcons.DojoQueryParamUIIcon)

  override def update(e: AnActionEvent): Unit =
    Option(SWITCHUI_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => e.getPresentation.setEnabledAndVisible(false)
      case Some(provider) =>
        if provider.getCurrentUI == DojoUI.Unauthenticated then
          e.getPresentation.setEnabledAndVisible(false)
        else
          e.getPresentation.setEnabledAndVisible(true)
          updateIcon(provider.getCurrentUI, e.getPresentation)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
