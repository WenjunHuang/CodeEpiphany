package com.wenjunhuang.codeepiphany.atcoder.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnActionEvent, DataKey }

import com.wenjunhuang.codeepiphany.model.CodeDojo.AtCoder
import com.wenjunhuang.codeepiphany.services.AuthService
import com.wenjunhuang.codeepiphany.utils.actions.AbstractLoadingAction

class AtCoderUpdateProblemSetsAction extends AbstractLoadingAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    Option(AtCoderUpdateProblemSetsAction.ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY.getData(e.getDataContext)) match {
      case Some(provider) =>
        if !provider.isUpdatingProblemSets then provider.updateProblemSets()
      case None =>
    }
  }

  override def update(e: AnActionEvent): Unit = {
    if e.getProject == null then e.getPresentation.setEnabledAndVisible(false)
    else if !AuthService.getInstance(e.getProject).isLoggedIn(AtCoder) then
      e.getPresentation.setEnabledAndVisible(false)
    else
      Option(AtCoderUpdateProblemSetsAction.ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY.getData(e.getDataContext)) match
        case None => e.getPresentation.setEnabled(false)
        case Some(provider) =>
          if provider.isUpdatingProblemSets then
            e.getPresentation.setEnabled(false)
            setLoading(e.getPresentation, true)
          else
            e.getPresentation.setEnabled(true)
            setLoading(e.getPresentation, false)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

}
object AtCoderUpdateProblemSetsAction {
  val ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY: DataKey[AtCoderUpdateProblemSetsProvider] =
    DataKey.create[AtCoderUpdateProblemSetsProvider]("ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY")

  trait AtCoderUpdateProblemSetsProvider {
    def updateProblemSets(): Unit

    def isUpdatingProblemSets: Boolean
  }
}
