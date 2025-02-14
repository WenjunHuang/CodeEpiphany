package com.wenjunhuang.codeepiphany.leetcode.actions

import icons.CodeEpiphanyIcons
import javax.swing.Icon

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction

import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeChangeUIAction.{
  LEETCODE_CHANGE_UI_PROVIDER_KEY,
  LeetCodeUI
}
import com.wenjunhuang.codeepiphany.leetcode.actions.LeetCodeChangeUIAction.LeetCodeUI.*
import com.wenjunhuang.codeepiphany.utils.actions.{ ActionCompatible, DataKeyNotNull }
import com.wenjunhuang.codeepiphany.PluginBundle

class LeetCodeChangeUIAction
    extends DefaultActionGroup
    with DataKeyNotNull(LEETCODE_CHANGE_UI_PROVIDER_KEY)
    with ActionCompatible {

  init()

  private def init(): Unit = {
    add(
      createSubAction(
        LeetCodeUI.QueryParameters,
        CodeEpiphanyIcons.QUERY_PARAM,
        PluginBundle.message("ui.query.parameters")
      )
    )
    add(createSubAction(LeetCodeUI.SearchByKeyword, CodeEpiphanyIcons.SEARCH, PluginBundle.message("ui.query.keyword")))
    add(createSubAction(LeetCodeUI.CompanyQuery, CodeEpiphanyIcons.BUILDING, PluginBundle.message("ui.query.company")))
    setPopup(true)
  }

  private def createSubAction(ui: LeetCodeUI, icon: Icon, text: String): AnAction = {
    val action = new AnAction(text, text, icon)
      with DataKeyNotNull(LEETCODE_CHANGE_UI_PROVIDER_KEY)
      with ActionCompatible {
      override def actionPerformed(e: AnActionEvent): Unit =
        getValue(e).switchTo(ui)

      override def update(e: AnActionEvent): Unit = {
        if getValue(e).getCurrentUI == ui then e.getPresentation.setIcon(AllIcons.General.InspectionsOK)
        else e.getPresentation.setIcon(null)
      }
    }
    action
  }

  private def updateIconAndName(ui: LeetCodeUI, present: Presentation): Unit =
    ui match
      case Unauthenticated => ()
      case QueryParameters =>
        present.setIcon(CodeEpiphanyIcons.QUERY_PARAM)
      case SearchByKeyword =>
        present.setIcon(CodeEpiphanyIcons.SEARCH)
      case CompanyQuery =>
        present.setIcon(CodeEpiphanyIcons.BUILDING)

  override def update(e: AnActionEvent): Unit =
    if isSatisfied(e) then
      val provider = getValue(e)
      if provider.getCurrentUI == Unauthenticated then e.getPresentation.setEnabledAndVisible(false)
      else
        e.getPresentation.setEnabledAndVisible(true)
        updateIconAndName(provider.getCurrentUI, e.getPresentation)
    else e.getPresentation.setEnabledAndVisible(false)

}

object LeetCodeChangeUIAction {
  val LEETCODE_CHANGE_UI_PROVIDER_KEY: DataKey[LeetCodeChangeUIProvider] =
    DataKey.create[LeetCodeChangeUIProvider]("LEETCODE_CHANGE_UI_PROVIDER_KEY")

  enum LeetCodeUI {
    case Unauthenticated
    case QueryParameters
    case SearchByKeyword
    case CompanyQuery
  }

  trait LeetCodeChangeUIProvider {
    def switchTo(ui: LeetCodeUI): Unit

    def getCurrentUI: LeetCodeUI
  }
}
