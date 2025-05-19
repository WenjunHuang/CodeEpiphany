package com.wenjunhuang.codeepiphany.codeforces.actions

import icons.CodeEpiphanyIcons
import org.typelevel.ci.CIString

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction

import com.wenjunhuang.codeepiphany.codeforces.actions.CodeForcesChangeUIAction.*
import com.wenjunhuang.codeepiphany.codeforces.actions.CodeForcesChangeUIAction.CodeForcesUI.*
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull}

class CodeForcesChangeUIAction
    extends DumbAwareAction
    with DataKeyNotNull(CODEFORCES_CHANGE_UI_PROVIDER_KEY)
    with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val provider = getValue(e)
    provider.getCurrentUI match
      case QueryParameters => provider.switchTo(SearchByKeyword)
      case SearchByKeyword => provider.switchTo(QueryParameters)
      case _               => ()
  }

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
    if isSatisfied(e) then
      val provider = getValue(e)
      if provider.getCurrentUI == Unauthenticated then e.getPresentation.setEnabledAndVisible(false)
      else
        e.getPresentation.setEnabledAndVisible(true)
        updateIconAndName(provider.getCurrentUI, e.getPresentation)
    else e.getPresentation.setEnabledAndVisible(false)

}

object CodeForcesChangeUIAction {
  val CODEFORCES_CHANGE_UI_PROVIDER_KEY: DataKey[CodeForcesChangeUIProvider] =
    DataKey.create[CodeForcesChangeUIProvider]("CODEFORCES_CHANGE_UI_PROVIDER_KEY")

  enum CodeForcesUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
  }

  object CodeForcesUI {
    def fromCIStringToAuthenticated(value:CIString):Option[CodeForcesUI] =
        if value == CIString(QueryParameters.toString) then Some(QueryParameters)
        else if value == CIString(SearchByKeyword.toString) then Some(SearchByKeyword)
        else None
  }

  trait CodeForcesChangeUIProvider {
    def switchTo(ui: CodeForcesUI): Unit

    def getCurrentUI: CodeForcesUI
  }
}
