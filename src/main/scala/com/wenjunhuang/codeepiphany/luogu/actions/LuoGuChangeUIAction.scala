package com.wenjunhuang.codeepiphany.luogu.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.wenjunhuang.codeepiphany.luogu.actions.LuoGuChangeUIAction.*
import com.wenjunhuang.codeepiphany.luogu.actions.LuoGuChangeUIAction.LuoGuUI.*
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull}
import icons.CodeEpiphanyIcons
import org.typelevel.ci.CIString

class LuoGuChangeUIAction
    extends DumbAwareAction
    with DataKeyNotNull(LUOGU_CHANGE_UI_PROVIDER_KEY)
    with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val provider = getValue(e)
    provider.getCurrentUI match
      case QueryParameters => provider.switchTo(SearchByKeyword)
      case SearchByKeyword => provider.switchTo(QueryParameters)
      case _               => ()
  }

  private def updateIconAndName(ui: LuoGuUI, present: Presentation): Unit = {
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

}

object LuoGuChangeUIAction {
  val LUOGU_CHANGE_UI_PROVIDER_KEY: DataKey[LuoGuChangeUIProvider] =
    DataKey.create[LuoGuChangeUIProvider]("LUOGU_CHANGE_UI_PROVIDER_KEY")

  enum LuoGuUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
  }

  object LuoGuUI {
    def fromCIStringToAuthenticated(value: CIString): Option[LuoGuUI] =
      if value == CIString(QueryParameters.toString) then Some(QueryParameters)
      else if value == CIString(SearchByKeyword.toString) then Some(SearchByKeyword)
      else None
  }

  trait LuoGuChangeUIProvider {
    def switchTo(ui: LuoGuUI): Unit

    def getCurrentUI: LuoGuUI
  }
}
