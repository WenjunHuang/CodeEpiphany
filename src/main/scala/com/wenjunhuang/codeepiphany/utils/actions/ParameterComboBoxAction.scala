package com.wenjunhuang.codeepiphany.utils.actions

import com.intellij.openapi.actionSystem.ex.{ CheckboxAction, ComboBoxAction }
import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnActionEvent, DataContext, DataKey, DefaultActionGroup }
import com.wenjunhuang.codeepiphany.utils.actions.ParameterComboBoxAction.QueryParamSubAction

import javax.swing.{ Icon, JComponent }

abstract class ParameterComboBoxAction[P, T <: ParameterProvider[P]](
  private val key: DataKey[T],
  private val name: P => String,
  private val description: P => Option[String],
  private val icon: P => Option[Icon]
) extends ComboBoxAction {
  override def update(e: AnActionEvent): Unit = {
    Option(key.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case _    => e.getPresentation.setEnabled(true)
    }
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

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object ParameterComboBoxAction {
  class QueryParamSubAction[P, T <: ParameterProvider[P]](
    val myData: P,
    val key: DataKey[T],
    val name: String,
    val description: Option[String],
    val icon: Option[Icon]
  ) extends CheckboxAction(name, description.orNull, icon.orNull) {
    override def isSelected(e: AnActionEvent): Boolean =
      Option(key.getData(e.getDataContext))
        .map(_.isSelected(myData))
        .getOrElse(false)

    override def setSelected(e: AnActionEvent, state: Boolean): Unit =
      Option(key.getData(e.getDataContext)).foreach { provider =>
        if state then provider.addSelectedItems(List(myData))
        else provider.removeSelectedItems(List(myData))
      }

    override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
  }
}
