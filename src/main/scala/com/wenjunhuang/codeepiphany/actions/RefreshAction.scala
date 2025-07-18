package com.wenjunhuang.codeepiphany.actions

import com.intellij.openapi.actionSystem.{AnActionEvent, DataKey}
import com.wenjunhuang.codeepiphany.utils.actions.{AbstractLoadingAction, ActionCompatible, DataKeyNotNull}

class RefreshAction
    extends AbstractLoadingAction
    with DataKeyNotNull(RefreshAction.REFRESH_PROVIDER_KEY)
    with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit = {
    getValue(e).refresh()
  }

  override def update(e: AnActionEvent): Unit = {
    if isSatisfied(e) then
      val provider = getValue(e)
      if provider.isRefreshing then
        e.getPresentation.setEnabled(false)
        setLoading(e.getPresentation, true)
      else setLoading(e.getPresentation, false)
    else e.getPresentation.setEnabled(false)
  }

}

object RefreshAction {
  val REFRESH_PROVIDER_KEY: DataKey[RefreshProvider] = DataKey.create[RefreshProvider]("REFRESH_PROVIDER_KEY")
  trait RefreshProvider {
    def refresh(): Unit
    def isRefreshing: Boolean
  }
}
