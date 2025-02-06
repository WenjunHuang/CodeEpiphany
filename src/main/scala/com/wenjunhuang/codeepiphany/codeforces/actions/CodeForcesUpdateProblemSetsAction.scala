package com.wenjunhuang.codeepiphany.codeforces.actions

import icons.CodeEpiphanyIcons

import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent, DataKey}

import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.services.AuthService

class CodeForcesUpdateProblemSetsAction extends AnAction {
  override def actionPerformed(e: AnActionEvent): Unit = {
    Option(
      CodeForcesUpdateProblemSetsAction.CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY.getData(e.getDataContext)
    ) match {
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
      Option(
        CodeForcesUpdateProblemSetsAction.CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY.getData(e.getDataContext)
      ) match
        case None => e.getPresentation.setEnabled(false)
        case Some(provider) =>
          if provider.isUpdatingProblemSets then
            e.getPresentation.setIcon(CodeEpiphanyIcons.LOADING)
            e.getPresentation.setEnabled(false)
          else
            e.getPresentation.setIcon(CodeEpiphanyIcons.DOWNLOAD)
            e.getPresentation.setEnabled(true)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

}
object CodeForcesUpdateProblemSetsAction {
  val CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY: DataKey[CodeForcesUpdateProblemSetsProvider] =
    DataKey.create[CodeForcesUpdateProblemSetsProvider]("CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY")

  trait CodeForcesUpdateProblemSetsProvider {
    def updateProblemSets(): Unit

    def isUpdatingProblemSets: Boolean
  }
}
