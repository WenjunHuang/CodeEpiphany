package com.wenjunhuang.codeepiphany.utils.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionManager, ActionToolbar, AnAction, DefaultActionGroup}
import com.intellij.openapi.actionSystem.ex.DefaultCustomComponentAction
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.ui.{Gray, InplaceButton, JBColor}
import com.intellij.ui.components.{JBLabel, JBLayeredPane}
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.{JBInsets, JBUI}
import com.intellij.util.ui.components.BorderLayoutPanel

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.{Icon, JLayeredPane, JPanel, SwingConstants}

class TagPane(val noBorderTop: Boolean = true) extends BorderLayoutPanel {

  val myActionGroup = DefaultActionGroup()
  private val myTagToolbar: ActionToolbar =
    ActionManager.getInstance().createActionToolbar("TagPane", myActionGroup, true)

  myTagToolbar.setTargetComponent(this)
  myTagToolbar.setLayoutStrategy(ToolbarLayoutStrategy.WRAP_STRATEGY)
  myTagToolbar.setReservePlaceAutoPopupIcon(false)

  if noBorderTop then
    val toolbarComp = myTagToolbar.getComponent
    val border      = toolbarComp.getBorder.getBorderInsets(toolbarComp)
    toolbarComp.setBorder(JBUI.Borders.empty(0, border.left, border.bottom, border.right))

  private def createTagAction(
    id: String,
    text: String,
    icon: Option[Icon],
    radius: Float,
    onCloseAction: Option[() => Unit]
  ): AnAction = DefaultCustomComponentAction { () =>
    val tag = JPanel(GridBagLayout())
    val gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weightx = 1.0
    gbc.weighty = 1.0
    gbc.insets = JBInsets.create(2, 2)
    tag.add(Tag(id, text, icon, radius, onCloseAction), gbc)
    tag
  }

  def removeAllTags(): Unit = myActionGroup.removeAll()

  @RequiresEdt
  def addTagAction(
    id: String,
    text: String,
    icon: Option[Icon],
    radius: Float,
    onCloseAction: Option[() => Unit]
  ): Unit = {
    myActionGroup.add(createTagAction(id, text, icon, radius, onCloseAction))
  }

  @RequiresEdt
  def updateActionsAsync(): Unit = {
    if myActionGroup.getChildrenCount == 0 then remove(myTagToolbar.getComponent)
    else if (0 until getComponentCount).exists(i => getComponent(i) == myTagToolbar.getComponent) then
      myTagToolbar.updateActionsAsync()
    else
      add(myTagToolbar.getComponent, SwingConstants.CENTER)
      revalidate()
      repaint()
  }
}

class Tag(val id: String, text: String, icon: Option[Icon], radius: Float, onCloseAction: Option[() => Unit])
    extends JBLayeredPane {
  import Tag.*
  private val myRadius = math.max(0.0f, math.min(1.0f, radius))
  private val myLabel  = JBLabel(text, icon.orNull, SwingConstants.LEFT)
  myLabel.setFont(JBUI.Fonts.toolbarSmallComboBoxFont())
  myLabel.setVerticalAlignment(SwingConstants.CENTER)
  myLabel.setVerticalTextPosition(SwingConstants.CENTER)
  add(myLabel, JLayeredPane.DEFAULT_LAYER)

  private val myCloseButton =
    onCloseAction.map(action =>
      new InplaceButton(
        IconButton(null, AllIcons.Actions.Close, AllIcons.Actions.CloseDarkGrey),
        (e: ActionEvent) => action()
      )
    )
  myCloseButton.foreach(add(_, JLayeredPane.POPUP_LAYER))

  setBackground(Tag.backgroundColor)
  layoutContent()

  private def layoutContent(): Unit = {
    val labelSize = myLabel.getPreferredSize
    val iconSize  = myCloseButton.map(_.getPreferredSize).getOrElse(Dimension(0, 0))
    val height    = math.max(labelSize.height, iconSize.height)
    val tagSize =
      Dimension(labelSize.width + iconSize.width + Tag.padding.width() + Tag.textIconGap, height + padding.height())
    setPreferredSize(tagSize)

    myLabel.setBounds(
      Tag.padding.left,
      (height - labelSize.height) / 2 + Tag.padding.top,
      labelSize.width,
      labelSize.height
    )
    myCloseButton.foreach(
      _.setBounds(
        tagSize.width - iconSize.width - Tag.padding.right,
        (height - iconSize.height) / 2 + Tag.padding.top,
        iconSize.width,
        iconSize.height
      )
    )
  }

  override def paint(g: Graphics): Unit = {
    val size = getSize
    g.setColor(Tag.backgroundColor)
    val radius = myRadius * math.min(size.width, size.height)
    g.fillRoundRect(0, 0, size.width, size.height, radius.toInt, radius.toInt)
    paintChildren(g)
  }
}

object Tag {
  final private val padding          = JBInsets.create(3, 8)
  final private val textIconGap      = JBUI.scale(4)
  private def backgroundColor: Color = JBColor.namedColor("Tag.background", Gray.xDF)
}
