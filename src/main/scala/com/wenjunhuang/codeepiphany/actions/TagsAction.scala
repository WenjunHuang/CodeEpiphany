package com.wenjunhuang.codeepiphany.actions

import java.awt.Dimension
import javax.swing.{ JComponent, SwingConstants }

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.{ CheckboxAction, ComboBoxAction }
import com.intellij.openapi.ui.popup.{ JBPopup, JBPopupFactory }
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.{ JBLabel, JBTabbedPane }

import com.wenjunhuang.codeepiphany.actions.TagsAction.*
import com.wenjunhuang.codeepiphany.utils.actions.ParameterProvider

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

class TagSubAction(private val myTag: Tag) extends CheckboxAction(myTag.name) {

  override def isSelected(e: AnActionEvent): Boolean =
    Option(TAG_PROVIDER_KEY.getData(e.getDataContext))
      .map(_.isSelected(myTag))
      .getOrElse(false)

  override def setSelected(e: AnActionEvent, state: Boolean): Unit =
    Option(TAG_PROVIDER_KEY.getData(e.getDataContext)).foreach { provider =>
      if state then provider.addSelectedItems(List(myTag))
      else provider.removeSelectedItems(List(myTag))
    }
  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}

object TagsAction {
  val TAG_PROVIDER_KEY: DataKey[TagProvider] = DataKey.create[TagProvider]("TAG_PROVIDER_KEY")

  case class TagGroup(name: String, value: String, tags: List[Tag])
  case class Tag(name: String, value: String, groupValue: String)

  sealed trait TagProvider extends ParameterProvider[Tag] {}

  trait SingleTagGroupProvider extends TagProvider {}

  trait MultiTagGroupProvider extends TagProvider {
    def isSearchEnabled: Boolean
    def searchTags(query: String): List[Tag]
    def getGroups: List[TagGroup]
  }
}
