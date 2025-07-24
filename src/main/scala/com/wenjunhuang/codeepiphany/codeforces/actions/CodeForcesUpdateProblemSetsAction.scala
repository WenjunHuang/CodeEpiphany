package com.wenjunhuang.codeepiphany.codeforces.actions

import com.intellij.openapi.actionSystem.{AnActionEvent, DataKey}
import com.wenjunhuang.codeepiphany.codeforces.actions.CodeForcesUpdateProblemSetsAction.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.CodeForces
import com.wenjunhuang.codeepiphany.utils.actions.{AbstractLoadingAction, ActionCompatible, DataKeyNotNull, UserLoggedIn}

class CodeForcesUpdateProblemSetsAction
    extends AbstractLoadingAction
    with DataKeyNotNull(CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY)
    with UserLoggedIn(CodeForces)
    with ActionCompatible {
  override def actionPerformed(e: AnActionEvent): Unit = {
    val provider = getValue(e)
    if !provider.isUpdatingProblemSets then provider.updateProblemSets()
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
}

object CodeForcesUpdateProblemSetsAction {
  val CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY: DataKey[CodeForcesUpdateProblemSetsProvider] =
    DataKey.create[CodeForcesUpdateProblemSetsProvider]("CODEFORCES_UPDATE_PROBLEM_SETS_PROVIDER_KEY")

  trait CodeForcesUpdateProblemSetsProvider {
    def updateProblemSets(): Unit

    def isUpdatingProblemSets: Boolean
  }
}
