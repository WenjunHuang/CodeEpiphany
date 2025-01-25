package com.wenjunhuang.codeepiphany.actions

import java.awt.{Dimension, GridBagConstraints, GridBagLayout}
import javax.swing.*

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.{CheckboxAction, ComboBoxAction}
import com.intellij.openapi.ui.popup.{JBPopup, JBPopupFactory}
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.{JBScrollPane, JBTabbedPane}
import com.intellij.ui.dsl.builder.impl.CollapsibleTitledSeparatorImpl
import com.intellij.uiDesigner.core.Spacer
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.JBUI

import com.wenjunhuang.codeepiphany.actions.TagsAction.*
import com.wenjunhuang.codeepiphany.utils.actions.ParameterProvider
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.ui.{TagPane, TagPaneAction}

class TagsAction extends ComboBoxAction {
  override def update(e: AnActionEvent): Unit =
    Option(TAG_PROVIDER_KEY.getData(e.getDataContext)) match {
      case None => e.getPresentation.setEnabled(false)
      case _    => e.getPresentation.setEnabled(true)
    }

  override def createActionPopup(context: DataContext, component: JComponent, disposeCallback: Runnable): JBPopup =
    Option(TAG_PROVIDER_KEY.getData(context)) match {
      case Some(provider) if provider.isInstanceOf[MultiTagGroupProvider] =>
        multiTagGroupPopup(provider.asInstanceOf[MultiTagGroupProvider], disposeCallback)
      case _ => super.createActionPopup(context, component, disposeCallback)
    }

  private def multiTagGroupPopup(provider: MultiTagGroupProvider, disposeCallback: Runnable): JBPopup = {
    val tabs       = provider.getTabs
    val tabbedPane = JBTabbedPane(SwingConstants.TOP)
    tabbedPane.setTabComponentInsets(JBUI.emptyInsets())
    tabbedPane.setMaximumSize(new Dimension(300, 400))
    tabs.foreach { tab =>
      tabbedPane.add(tab.name, createTagGroupTab(provider, tab))
    }
    val popup = JBPopupFactory
      .getInstance()
      .createComponentPopupBuilder(tabbedPane, tabbedPane)
      .setCancelOnClickOutside(true)
      .setMinSize(new Dimension(300, 300))
      .setResizable(true)
      .createPopup()
    Disposer.register(popup, () => disposeCallback.run())
    popup
  }

  private def createTagGroupTab(provider: MultiTagGroupProvider, tab: TagGroupTab): JComponent = {
    val panel = new JPanel()

    panel.setLayout(new GridBagLayout())

    val gbc = new GridBagConstraints()
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.weightx = 1.0
    gbc.gridx = 0
    tab.tagGroups.zipWithIndex.foreach { (tagGroup, index) =>
      val actionTags = tagGroup.tags.map { tag =>
        TagPaneAction(
          tag.value,
          tag.name,
          None,
          0.5f,
          Some(
            { () => provider.isSelected(tag) },
            { selected =>
              if selected then provider.addSelectedItems(List(tag))
              else provider.removeSelectedItems(List(tag))
            }
          ),
          None
        )
      }
      val tagGroupPane = TagPane(actions = actionTags)
      val titled       = new CollapsibleTitledSeparatorImpl(tagGroup.name)
      titled.onAction { isExpanded =>
        if isExpanded then tagGroupPane.setVisible(true)
        else tagGroupPane.setVisible(false)
      }
      titled.setExpanded(true)
      tagGroupPane.updateActionsAsync()

      panel.add(
        BorderLayoutPanel()
          .addToCenter(tagGroupPane)
          .addToTop(titled),
        gbc
      )
    }

    gbc.fill = GridBagConstraints.BOTH
    gbc.weighty = 1.0
    panel.add(Spacer(), gbc)

    JBScrollPane(
      panel,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    )

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

  case class TagGroup(name: String, value: String, tags: List[Tag], userObj: Any = null)
  case class Tag(name: String, value: String, groupValue: String, userObj: Any = null)
  case class TagGroupTab(name: String, value: String, tagGroups: List[TagGroup], userObj: Any = null)

  sealed trait TagProvider extends ParameterProvider[Tag] {}

  trait SingleTagGroupProvider extends TagProvider {}

  trait MultiTagGroupProvider extends TagProvider {
    def isSearchEnabled: Boolean
    def searchTags(query: String): List[Tag]
    def getTabs: List[TagGroupTab]
  }
}
