package com.wenjunhuang.codeepiphany.leetcode.actions

import icons.CodeEpiphanyIcons

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction

import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeChangeUIAction.{
  LEETCODE_CHANGE_UI_PROVIDER_KEY,
  LeetCodeUI
}
import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeChangeUIAction.LeetCodeUI.*

class LeetCodeChangeUIAction extends DumbAwareAction {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(LEETCODE_CHANGE_UI_PROVIDER_KEY.getData(e.getDataContext)) match
      case Some(provider) =>
        provider.getCurrentUI match
          case QueryParameters => provider.switchTo(SearchByKeyword)
          case SearchByKeyword => provider.switchTo(QueryParameters)
          case _               => ()
      case None => ()

  private def updateIconAndName(ui: LeetCodeUI, present: Presentation): Unit =
    ui match
      case Unauthenticated => ()
      case QueryParameters =>
        present.setIcon(CodeEpiphanyIcons.SEARCH)
        present.setText("Search by Keyword")
      case SearchByKeyword =>
        present.setIcon(CodeEpiphanyIcons.QUERY_PARAM)
        present.setText("Query Parameters")

  override def update(e: AnActionEvent): Unit =
    Option(LEETCODE_CHANGE_UI_PROVIDER_KEY.getData(e.getDataContext)) match
      case None => e.getPresentation.setEnabledAndVisible(false)
      case Some(provider) =>
        if provider.getCurrentUI == Unauthenticated then e.getPresentation.setEnabledAndVisible(false)
        else
          e.getPresentation.setEnabledAndVisible(true)
          updateIconAndName(provider.getCurrentUI, e.getPresentation)

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object LeetCodeChangeUIAction {
  val LEETCODE_CHANGE_UI_PROVIDER_KEY: DataKey[LeetCodeChangeUIProvider] =
    DataKey.create[LeetCodeChangeUIProvider]("LEETCODE_CHANGE_UI_PROVIDER_KEY")

  enum LeetCodeUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
  }

  trait LeetCodeChangeUIProvider {
    def switchTo(ui: LeetCodeUI): Unit

    def getCurrentUI: LeetCodeUI
  }
}
