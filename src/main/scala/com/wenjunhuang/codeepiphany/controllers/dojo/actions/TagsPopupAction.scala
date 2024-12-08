package com.wenjunhuang.codeepiphany.controllers.dojo.actions

import com.intellij.openapi.actionSystem.{ ActionUpdateThread, AnAction, AnActionEvent, DataContext, DefaultActionGroup }
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.ui.popup.{ JBPopup, JBPopupFactory, ListPopup }
import com.intellij.ui.components.{ JBLabel, JBTabbedPane }
import com.intellij.openapi.ui.popup.util.PopupUtil

import javax.swing.{ JComponent, SwingConstants }
import java.awt.Dimension

class TagsPopupAction extends ComboBoxAction {
  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setEnabledAndVisible(true)

  override def createActionPopup(context: DataContext, component: JComponent, disposeCallback: Runnable): JBPopup = {
    val tabbedPane = JBTabbedPane(SwingConstants.TOP)
    tabbedPane.insertTab("Companies", null, JBLabel("Companies"), "Companies", 0)
    tabbedPane.insertTab("Topics", null, JBLabel("Topics"), "Topics", 0)
    val popup = JBPopupFactory
      .getInstance()
      .createComponentPopupBuilder(tabbedPane, null)
      .setStretchToOwnerWidth(true)
      .setResizable(true)
      .setCancelOnClickOutside(true)
      .setMinSize(new Dimension(getMinWidth, getMinHeight))
      .createPopup()
    popup
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT

  override def actionPerformed(e: AnActionEvent): Unit = {
    // Do nothing
  }
}
