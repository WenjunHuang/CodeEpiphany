package com.wenjunhuang.codeepiphany.codeforces.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnActionEvent, DataKey }

import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.services.AuthService
import com.wenjunhuang.codeepiphany.utils.actions.AbstractLoadingAction
import CodeForcesUpdateProblemSetsAction.*

class CodeForcesUpdateProblemSetsAction extends AbstractLoadingAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    getProvider(e) match {
      case Some(provider) =>
        if !provider.isUpdatingProblemSets then provider.updateProblemSets()
      case None =>
    }
  }

  override def update(e: AnActionEvent): Unit = {
    if e.getProject == null then e.getPresentation.setEnabledAndVisible(false)
    else if !AuthService.getInstance(e.getProject).isLoggedIn(CodeForces) then
      e.getPresentation.setEnabledAndVisible(false)
    else
      getProvider(e) match
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

  private def getProvider(e: AnActionEvent) = {
    Option(CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY.getData(e.getDataContext))
  }
}
object CodeForcesUpdateProblemSetsAction {
  val CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY: DataKey[CodeForcesUpdateProblemSetsProvider] =
    DataKey.create[CodeForcesUpdateProblemSetsProvider]("CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY")

  trait CodeForcesUpdateProblemSetsProvider {
    def updateProblemSets(): Unit

    def isUpdatingProblemSets: Boolean
  }
}
