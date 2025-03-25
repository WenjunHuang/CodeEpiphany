package com.wenjunhuang.codeepiphany.hackerrank.actions

import icons.CodeEpiphanyIcons
import org.typelevel.ci.CIString

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction

import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.*
import com.wenjunhuang.codeepiphany.hackerrank.actions.ChangeChallengesUIAction.HackerRankUI.*
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull, UserLoggedIn}

class ChangeChallengesUIAction
    extends DumbAwareAction
    with DataKeyNotNull(CHANGE_CHALLENGES_UI_PROVIDER_KEY)
    with UserLoggedIn(HackerRank)
    with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val provider = getValue(e)
    provider.getCurrentUI match
      case QueryParameters => provider.switchTo(SearchByKeyword)
      case SearchByKeyword => provider.switchTo(QueryParameters)
      case _               => ()
  }

  private def updateIconAndName(ui: HackerRankUI, present: Presentation): Unit =
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

}

object ChangeChallengesUIAction {
  val CHANGE_CHALLENGES_UI_PROVIDER_KEY: DataKey[ChangeChallengesUIProvider] =
    DataKey.create[ChangeChallengesUIProvider]("CHANGE_CHALLENGES_UI_PROVIDER_KEY")

  enum HackerRankUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
  }

  object HackerRankUI {
    def fromCIStringToAuthenticated(value: CIString): Option[HackerRankUI] =
      if value == CIString(QueryParameters.toString) then Some(QueryParameters)
      else if value == CIString(SearchByKeyword.toString) then Some(SearchByKeyword)
      else None
  }

  trait ChangeChallengesUIProvider {
    def switchTo(ui: HackerRankUI): Unit

    def getCurrentUI: HackerRankUI
  }
}
