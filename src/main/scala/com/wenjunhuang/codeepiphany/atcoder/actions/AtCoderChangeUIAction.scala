package com.wenjunhuang.codeepiphany.atcoder.actions

import icons.CodeEpiphanyIcons

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction

import AtCoderChangeUIAction.*
import AtCoderUI.*
import com.wenjunhuang.codeepiphany.utils.actions.DataKeyNotNull

class AtCoderChangeUIAction extends DumbAwareAction with DataKeyNotNull(ATCODER_CHANGE_UI_PROVIDER_KEY) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val provider = getValue(e)
    provider.getCurrentUI match
      case QueryParameters => provider.switchTo(SearchByKeyword)
      case SearchByKeyword => provider.switchTo(QueryParameters)
      case _               => ()
  }

  private def updateIconAndName(ui: AtCoderUI, present: Presentation): Unit = {
    ui match
      case Unauthenticated => ()
      case QueryParameters =>
        present.setIcon(CodeEpiphanyIcons.SEARCH)
        present.setText("Search by Keyword")
      case SearchByKeyword =>
        present.setIcon(CodeEpiphanyIcons.QUERY_PARAM)
        present.setText("Query Parameters")
  }

  override def update(e: AnActionEvent): Unit =
    if isSatisfied(e) then
      val provider = getValue(e)
      if provider.getCurrentUI == Unauthenticated then e.getPresentation.setEnabledAndVisible(false)
      else
        e.getPresentation.setEnabledAndVisible(true)
        updateIconAndName(provider.getCurrentUI, e.getPresentation)
    else e.getPresentation.setEnabledAndVisible(false)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object AtCoderChangeUIAction {
  val ATCODER_CHANGE_UI_PROVIDER_KEY: DataKey[AtCoderChangeUIProvider] =
    DataKey.create[AtCoderChangeUIProvider]("ATCODER_CHANGE_UI_PROVIDER_KEY")

  enum AtCoderUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
  }

  trait AtCoderChangeUIProvider {
    def switchTo(ui: AtCoderUI): Unit

    def getCurrentUI: AtCoderUI
  }
}
