package com.wenjunhuang.codeepiphany.codeforces.actions

import icons.CodeEpiphanyIcons

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction

import CodeForcesChangeUIAction.*
import com.wenjunhuang.codeepiphany.codeforces.actions.CodeForcesChangeUIAction.CodeForcesUI.*

class CodeForcesChangeUIAction extends DumbAwareAction {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(CODEFORCES_CHANGE_UI_PROVIDER_KEY.getData(e.getDataContext)) match
      case Some(provider) =>
        provider.getCurrentUI match
          case QueryParameters => provider.switchTo(SearchByKeyword)
          case SearchByKeyword => provider.switchTo(QueryParameters)
          case _               => ()
      case None => ()

  private def updateIconAndName(ui: CodeForcesUI, present: Presentation): Unit =
    ui match
      case Unauthenticated => ()
      case QueryParameters =>
        present.setIcon(CodeEpiphanyIcons.SEARCH)
        present.setText("Search by Keyword")
      case SearchByKeyword =>
        present.setIcon(CodeEpiphanyIcons.QUERY_PARAM)
        present.setText("Query Parameters")

  override def update(e: AnActionEvent): Unit =
    Option(CODEFORCES_CHANGE_UI_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => e.getPresentation.setEnabledAndVisible(false)
      case Some(provider) =>
        if provider.getCurrentUI == Unauthenticated then e.getPresentation.setEnabledAndVisible(false)
        else
          e.getPresentation.setEnabledAndVisible(true)
          updateIconAndName(provider.getCurrentUI, e.getPresentation)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object CodeForcesChangeUIAction {
  val CODEFORCES_CHANGE_UI_PROVIDER_KEY: DataKey[CodeForcesChangeUIProvider] =
    DataKey.create[CodeForcesChangeUIProvider]("CODEFORCES_CHANGE_UI_PROVIDER_KEY")

  enum CodeForcesUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
  }

  trait CodeForcesChangeUIProvider {
    def switchTo(ui: CodeForcesUI): Unit

    def getCurrentUI: CodeForcesUI
  }
}
