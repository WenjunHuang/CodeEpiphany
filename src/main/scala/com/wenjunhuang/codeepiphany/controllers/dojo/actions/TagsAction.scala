package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent, DataContext, DefaultActionGroup }
import com.intellij.openapi.ui.popup.{ JBPopup, JBPopupFactory }
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.{ JBLabel, JBTabbedPane }
import com.wenjunhuang.codeepiphany.controllers.dojo.actions.keys.{ LISTS_PROVIDER_KEY, TAG_PROVIDER_KEY }

import java.awt.Dimension
import javax.swing.{ JComponent, SwingConstants }

class TagsAction extends ComboBoxAction {
  override def update(e: AnActionEvent): Unit =
    Option(TAG_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case _    => e.getPresentation.setEnabled(true)
    }

  override def createActionPopup(context: DataContext, component: JComponent, disposeCallback: Runnable): JBPopup =
    Option(TAG_PROVIDER_KEY.getData(context)) match {
      case Some(provider) if provider.isInstanceOf[MultiTagGroupProvider] =>
        multiTagGroupPopup(disposeCallback)
      case _ => super.createActionPopup(context, component, disposeCallback)
    }

  private def multiTagGroupPopup(disposeCallback: Runnable): JBPopup = {
    val tabbedPane = JBTabbedPane(SwingConstants.TOP)
    tabbedPane.insertTab("Companies", null, JBLabel("Companies"), "Companies", 0)
    tabbedPane.insertTab("Topics", null, JBLabel("Topics"), "Topics", 0)
    val popup = JBPopupFactory
      .getInstance()
      .createComponentPopupBuilder(tabbedPane, null)
      .setStretchToOwnerWidth(true)
      .setResizable(true)
      .setCancelOnClickOutside(true)
      .setMinSize(new Dimension(300, getMinHeight))
      .createPopup()
    Disposer.register(popup, () => disposeCallback.run())
    popup
  }

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
    Option(TAG_PROVIDER_KEY.getData(dataContext))
      .map(_.getAllItems)
      .map(tags => tags.map(new TagSubAction(_)))
      .map(actions => new DefaultActionGroup(actions*))
      .getOrElse(new DefaultActionGroup())

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def actionPerformed(e: AnActionEvent): Unit = {}
}

class TagSubAction(private val myTag: Tag) extends AnAction(myTag.name) {
  override def actionPerformed(e: AnActionEvent): Unit =
    Option(TAG_PROVIDER_KEY.getData(e.getDataContext))
      .foreach(_.toggleSelection(myTag))

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    Option(TAG_PROVIDER_KEY.getData(e.getDataContext))
      .map(_.isSelected(myTag))
      .foreach {
        case true  => presentation.setIcon(AllIcons.Actions.Checked)
        case false => presentation.setIcon(null)
      }
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
