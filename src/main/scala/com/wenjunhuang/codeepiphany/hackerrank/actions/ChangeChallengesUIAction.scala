package com.wenjunhuang.codeepiphany.hackerrank.actions

import icons.CodeEpiphanyIcons

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction

import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.*
import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.ChallengesUI.*

class ChangeChallengesUIAction extends DumbAwareAction {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(CHANGE_CHALLENGES_UI_PROVIDER_KEY.getData(e.getDataContext)) match
      case Some(provider) =>
        provider.getCurrentUI match
          case QueryParameters => provider.switchTo(SearchByKeyword)
          case SearchByKeyword => provider.switchTo(QueryParameters)
          case _               => ()
      case None => ()

  private def updateIconAndName(ui: ChallengesUI, present: Presentation): Unit =
    ui match
      case Unauthenticated => ()
      case QueryParameters =>
        present.setIcon(CodeEpiphanyIcons.SEARCH)
        present.setText("Search by Keyword")
      case SearchByKeyword =>
        present.setIcon(CodeEpiphanyIcons.QUERY_PARAM)
        present.setText("Query Parameters")

  override def update(e: AnActionEvent): Unit =
    Option(CHANGE_CHALLENGES_UI_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => e.getPresentation.setEnabledAndVisible(false)
      case Some(provider) =>
        if provider.getCurrentUI == Unauthenticated then e.getPresentation.setEnabledAndVisible(false)
        else
          e.getPresentation.setEnabledAndVisible(true)
          updateIconAndName(provider.getCurrentUI, e.getPresentation)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object ChangeChallengesUIAction {
  val CHANGE_CHALLENGES_UI_PROVIDER_KEY: DataKey[ChangeChallengesUIProvider] =
    DataKey.create[ChangeChallengesUIProvider]("CHANGE_CHALLENGES_UI_PROVIDER_KEY")

  enum ChallengesUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
  }

  trait ChangeChallengesUIProvider {
    def switchTo(ui: ChallengesUI): Unit

    def getCurrentUI: ChallengesUI
  }
}
