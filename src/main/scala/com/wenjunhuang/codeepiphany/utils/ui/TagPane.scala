package com.wenjunhuang.codeepiphany.utils.ui

import java.awt.{ Color, Dimension, Graphics, GridBagConstraints, GridBagLayout }
import java.awt.event.{ ActionEvent, MouseAdapter, MouseEvent }
import javax.swing.{ Icon, JLayeredPane, JPanel, SwingConstants }

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ ActionManager, ActionToolbar, DefaultActionGroup }
import com.intellij.openapi.actionSystem.ex.DefaultCustomComponentAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.ui.{ Gray, InplaceButton, JBColor }
import com.intellij.ui.components.{ JBLabel, JBLayeredPane }
import com.intellij.util.ui.{ JBInsets, JBUI }
import com.intellij.util.ui.components.BorderLayoutPanel

import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

class TagPane(val noBorderTop: Boolean = true, private val myTagsModel: ObservableProperty[List[TagPaneAction]])
    extends BorderLayoutPanel {

  private val myActionGroup = DefaultActionGroup(myTagsModel.get()*)
  private val myTagToolbar =
    ActionManager.getInstance().createActionToolbar("TagPane", myActionGroup, true)

  myTagToolbar.setTargetComponent(this)
//  myTagToolbar.setLayoutStrategy(ToolbarLayoutStrategy.WRAP_STRATEGY)
  myTagToolbar.setLayoutPolicy(ActionToolbar.WRAP_LAYOUT_POLICY)
  myTagToolbar.setReservePlaceAutoPopupIcon(false)

  if noBorderTop then
    val toolbarComp = myTagToolbar.getComponent
    val border      = toolbarComp.getBorder.getBorderInsets(toolbarComp)
    toolbarComp.setBorder(JBUI.Borders.empty(0, border.left, border.bottom, border.right))

  updateActions()
  myTagsModel.afterChange(tags => {
    myActionGroup.removeAll()
    myActionGroup.addAll(tags*)
    ApplicationManager.getApplication.invokeLater { () =>
      updateActions()
      revalidate()
    }
  })

  private def updateActions(): Unit = {
    if myActionGroup.getChildrenCount == 0 then remove(myTagToolbar.getComponent)
    else if (0 until getComponentCount).exists(i => getComponent(i) == myTagToolbar.getComponent) then
      myTagToolbar.updateActionsImmediately()
    else add(myTagToolbar.getComponent, SwingConstants.CENTER)
  }
}

class TagPaneAction(
  id: String,
  text: String,
  icon: Option[Icon],
  radius: Float,
  onSelected: Option[(() => Boolean, (Boolean) => Unit)],
  onCloseAction: Option[() => Unit]
) extends DefaultCustomComponentAction({ () =>
      val tag = JPanel(GridBagLayout())
      val gbc = GridBagConstraints()
      gbc.gridx = 0
      gbc.gridy = 0
      gbc.weightx = 1.0
      gbc.weighty = 1.0
      gbc.insets = JBInsets.create(2, 2)
      tag.add(TagUI(id, text, icon, radius, onSelected, onCloseAction), gbc)
      tag
    }) {}

class TagUI(
  val id: String,
  text: String,
  icon: Option[Icon],
  radius: Float,
  private val myOnSelected: Option[(() => Boolean, (Boolean) => Unit)],
  private val myOnCloseAction: Option[() => Unit]
) extends JBLayeredPane {
  import TagUI.*
  private val myRadius = math.max(0.0f, math.min(1.0f, radius))
  private val myLabel  = JBLabel(text, icon.orNull, SwingConstants.LEFT)
  @volatile
  private var mySelected = myOnSelected.exists(_._1())

  myLabel.setFont(JBUI.Fonts.toolbarSmallComboBoxFont())
  myLabel.setVerticalAlignment(SwingConstants.CENTER)
  myLabel.setVerticalTextPosition(SwingConstants.CENTER)
  add(myLabel, JLayeredPane.DEFAULT_LAYER)

  private val myCloseButton =
    myOnCloseAction.map(action =>
      new InplaceButton(
        IconButton(null, AllIcons.Actions.Close, AllIcons.Actions.CloseDarkGrey),
        (e: ActionEvent) => action()
      )
    )
  myCloseButton.foreach(add(_, JLayeredPane.POPUP_LAYER))

  myOnSelected.foreach { action =>
    addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        mySelected = !mySelected
        action._2.apply(mySelected)
        repaint()
      }
    })
  }

  layoutContent()

  private def layoutContent(): Unit = {
    val labelSize = myLabel.getPreferredSize
    val iconSize  = myCloseButton.map(_.getPreferredSize).getOrElse(Dimension(0, 0))
    val height    = math.max(labelSize.height, iconSize.height)
    val tagSize =
      Dimension(labelSize.width + iconSize.width + TagUI.padding.width() + TagUI.textIconGap, height + padding.height())
    setPreferredSize(tagSize)

    myLabel.setBounds(
      TagUI.padding.left,
      (height - labelSize.height) / 2 + TagUI.padding.top,
      labelSize.width,
      labelSize.height
    )
    myCloseButton.foreach(
      _.setBounds(
        tagSize.width - iconSize.width - TagUI.padding.right,
        (height - iconSize.height) / 2 + TagUI.padding.top,
        iconSize.width,
        iconSize.height
      )
    )
  }

  def setText(text: String): Unit = {
    myLabel.setText(text)
    layoutContent()
    repaint()
  }

  def setIcon(icon: Icon): Unit = {
    myLabel.setIcon(icon)
    layoutContent()
    repaint()
  }

  override def paint(g: Graphics): Unit = {
    val size = getSize
    g.setColor(if mySelected then selectedBackgroundColor else backgroundColor)
    val radius = myRadius * math.min(size.width, size.height)
    g.fillRoundRect(0, 0, size.width, size.height, radius.toInt, radius.toInt)
    paintChildren(g)
  }
}

object TagUI {
  private final val padding                  = JBInsets.create(3, 8)
  private final val textIconGap              = JBUI.scale(4)
  def backgroundColor: Color                 = JBColor.namedColor("Tag.background", Gray.xDF).contrast(0.8)
  private def selectedBackgroundColor: Color = JBColor.namedColor("Tag.selectionBackground", Gray.xC9).contrast(1.2)
}
