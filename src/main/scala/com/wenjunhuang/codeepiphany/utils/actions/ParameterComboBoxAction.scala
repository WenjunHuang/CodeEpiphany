package com.wenjunhuang.codeepiphany.utils.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.{CheckboxAction, ComboBoxAction}
import com.wenjunhuang.codeepiphany.utils.actions.ParameterComboBoxAction.QueryParamSubAction

import javax.swing.{Icon, JComponent}

abstract class ParameterComboBoxAction[P, T <: ParameterProvider[P]](
  private val key: DataKey[T],
  private val name: P => String,
  private val description: P => Option[String],
  private val icon: P => Option[Icon]
) extends ComboBoxAction
    with DataKeyNotNull[T](key)
    with ActionCompatible {
  override def update(e: AnActionEvent): Unit = {
    if isSatisfied(e) then e.getPresentation.setEnabledAndVisible(true)
    else e.getPresentation.setEnabledAndVisible(false)
  }

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup = {
    Option(key.getData(dataContext))
      .map(_.getAllItems)
      .map(diffs =>
        diffs.map(item => new QueryParamSubAction[P, T](item, key, name(item), description(item), icon(item)))
      )
      .map(actions => new DefaultActionGroup(actions*))
      .getOrElse(new DefaultActionGroup())
  }
}

object ParameterComboBoxAction {
  class QueryParamSubAction[P, T <: ParameterProvider[P]](
    val myData: P,
    val key: DataKey[T],
    val name: String,
    val description: Option[String],
    val icon: Option[Icon]
  ) extends CheckboxAction(name, description.orNull, icon.orNull)
      with DataKeyNotNull[T](key)
      with ActionCompatible {
    override def isSelected(e: AnActionEvent): Boolean =
      getValue(e).isSelected(myData)

    override def setSelected(e: AnActionEvent, state: Boolean): Unit =
      if state then getValue(e).addSelectedItems(List(myData))
      else getValue(e).removeSelectedItems(List(myData))

    override def update(e: AnActionEvent): Unit = {
      if isSatisfied(e) then
        e.getPresentation.setEnabledAndVisible(true)
        super.update(e)
      else e.getPresentation.setEnabledAndVisible(false)
    }
  }
}
