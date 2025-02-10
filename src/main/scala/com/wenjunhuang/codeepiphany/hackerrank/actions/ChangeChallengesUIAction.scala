package com.wenjunhuang.codeepiphany.hackerrank.actions

import icons.CodeEpiphanyIcons

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction

import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.*
import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.ChallengesUI.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.utils.actions.{ DataKeyNotNull, UserLoggedIn }

class ChangeChallengesUIAction
    extends DumbAwareAction
    with DataKeyNotNull(CHANGE_CHALLENGES_UI_PROVIDER_KEY)
    with UserLoggedIn(HackerRank) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val provider = getValue(e)
    provider.getCurrentUI match
      case QueryParameters => provider.switchTo(SearchByKeyword)
      case SearchByKeyword => provider.switchTo(QueryParameters)
      case _               => ()
  }

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
    if isSatisfied(e) then
      val provider = getValue(e)
      if provider.getCurrentUI == Unauthenticated then e.getPresentation.setEnabledAndVisible(false)
      else
        e.getPresentation.setEnabledAndVisible(true)
        updateIconAndName(provider.getCurrentUI, e.getPresentation)
    else e.getPresentation.setEnabledAndVisible(false)

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
