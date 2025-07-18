package com.wenjunhuang.codeepiphany.utils.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.util.Key
import com.wenjunhuang.codeepiphany.editor.extensions.ChallengeEditorProvider
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

trait FileEditorAction extends ActionPrecondition {
  override def isSatisfied(event: AnActionEvent): Boolean = {
    if super.isSatisfied(event) then event.getDataContext.getData(PlatformCoreDataKeys.FILE_EDITOR) != null
    else false
  }

  def getEditor(event: AnActionEvent): FileEditor = getEditor(event.getDataContext)

  def getEditor(context: DataContext): FileEditor = context.getData(PlatformCoreDataKeys.FILE_EDITOR)
}

trait FileEditorUserLoggedIn extends ActionPrecondition with FileEditorAction {
  override def isSatisfied(event: AnActionEvent): Boolean = {
    if super.isSatisfied(event) then {
      getEditor(event).getUserData(ChallengeEditorProvider.FILEEDITOR_CODEDOJO_KEY) match {
        case null => false
        case codeDojo: CodeDojo =>
          AuthService.getInstance(event.getProject).isLoggedIn(codeDojo)
      }
    } else false
  }
}
trait FileEditorKeyNotNull[T](key: Key[T]) extends ActionPrecondition with FileEditorAction {
  override def isSatisfied(event: AnActionEvent): Boolean = {
    if super.isSatisfied(event) then getEditor(event).getUserData(key) != null
    else false
  }

  def getValue(event: AnActionEvent): T = getEditor(event).getUserData(key)
  def getValue(context: DataContext): T = getEditor(context).getUserData(key)
}
