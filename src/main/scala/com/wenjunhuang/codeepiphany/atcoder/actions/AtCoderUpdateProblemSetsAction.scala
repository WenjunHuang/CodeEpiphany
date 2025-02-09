package com.wenjunhuang.codeepiphany.atcoder.actions

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnActionEvent, DataKey}

import com.wenjunhuang.codeepiphany.atcoder.actions.AtCoderUpdateProblemSetsAction.ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY
import com.wenjunhuang.codeepiphany.model.CodeDojo.AtCoder
import com.wenjunhuang.codeepiphany.utils.actions.{AbstractLoadingAction, DataKeyNotNull, UserLoggedIn}

class AtCoderUpdateProblemSetsAction
    extends AbstractLoadingAction
    with DataKeyNotNull(ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY)
    with UserLoggedIn(AtCoder) {
  override def actionPerformed(e: AnActionEvent): Unit = {
    Option(AtCoderUpdateProblemSetsAction.ATCODER_UPDATE_PROBLEM_SETS_PROVIDER_KEY.getData(e.getDataContext)) match {
      case Some(provider) =>
        if !provider.isUpdatingProblemSets then provider.updateProblemSets()
      case None =>
    }
  }

  override def update(e: AnActionEvent): Unit = {
    if isSatisfied(e) then
      val provider = getValue(e)
      if provider.isUpdatingProblemSets then
        e.getPresentation.setEnabled(false)
        setLoading(e.getPresentation, true)
      else
        e.getPresentation.setEnabled(true)
        setLoading(e.getPresentation, false)
    else e.getPresentation.setEnabledAndVisible(false)
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
