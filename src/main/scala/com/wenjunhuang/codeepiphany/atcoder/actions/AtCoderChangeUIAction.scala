package com.wenjunhuang.codeepiphany.atcoder.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderChangeUIAction.*
import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderChangeUIAction.AtCoderUI.*
import com.wenjunhuang.codeepiphany.utils.actions.{ActionCompatible, DataKeyNotNull}
import icons.CodeEpiphanyIcons
import org.typelevel.ci.CIString

class AtCoderChangeUIAction
    extends DumbAwareAction
    with DataKeyNotNull(ATCODER_CHANGE_UI_PROVIDER_KEY)
    with ActionCompatible {
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

}

object AtCoderChangeUIAction {
  val ATCODER_CHANGE_UI_PROVIDER_KEY: DataKey[AtCoderChangeUIProvider] =
    DataKey.create[AtCoderChangeUIProvider]("ATCODER_CHANGE_UI_PROVIDER_KEY")

  enum AtCoderUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
  }

  object AtCoderUI {
    def fromCIStringToAuthenticated(value: CIString): Option[AtCoderUI] =
      if value == CIString("QueryParameters") then Some(QueryParameters)
      else if value == CIString("SearchByKeyword") then Some(SearchByKeyword)
      else None
  }

  trait AtCoderChangeUIProvider {
    def switchTo(ui: AtCoderUI): Unit

    def getCurrentUI: AtCoderUI
  }
}
