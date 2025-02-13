package com.wenjunhuang.codeepiphany.utils.actions

import com.intellij.openapi.actionSystem.{AnActionEvent, DataContext, DataKey}

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.AuthService

trait ActionPrecondition {
  def isSatisfied(event: AnActionEvent): Boolean = true
}

trait ProjectNonNull extends ActionPrecondition {
  override def isSatisfied(event: AnActionEvent): Boolean = {
    if super.isSatisfied(event) then event.getProject != null
    else false
  }
}

trait UserLoggedIn(codeDojo: CodeDojo) extends ProjectNonNull {
  override def isSatisfied(event: AnActionEvent): Boolean = {
    if super.isSatisfied(event) then AuthService.getInstance(event.getProject).isLoggedIn(codeDojo)
    else false
  }
}

trait DataKeyNotNull[T](key: DataKey[T]) extends ActionPrecondition {
  def getValue(event: AnActionEvent): T = getValue(event.getDataContext)
  def getValue(context: DataContext): T = key.getData(context)

  override def isSatisfied(event: AnActionEvent): Boolean = {
    if super.isSatisfied(event) then key.getData(event.getDataContext) != null
    else false
  }
}
