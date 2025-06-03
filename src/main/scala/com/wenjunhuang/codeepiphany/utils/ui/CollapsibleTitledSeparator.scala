package com.wenjunhuang.codeepiphany.utils.ui

import java.awt.Insets
import java.awt.event.{MouseAdapter, MouseEvent}
import kotlin.jvm.functions.Function1

import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.{IndentedIcon, UIUtil}

import com.wenjunhuang.codeepiphany.utils.syntax.*

class CollapsibleTitledSeparator(title: String,expanded:Boolean = true) extends TitledSeparator(title) {
  private val myExpanded = AtomicBooleanProperty(expanded)

  updateIcon()
  myExpanded.afterChange(p1 => updateIcon())
  addMouseListener(new MouseAdapter {
    override def mouseReleased(e: MouseEvent): Unit = {
      myExpanded.set(!myExpanded.get())
    }
  })

  def onAction(listener: Boolean => Unit): Unit = {
    myExpanded.afterChange(p1 => listener(p1))
  }
  
  def setExpanded(expanded: Boolean): Unit = {
    myExpanded.set(expanded)
  }

  private def updateIcon(): Unit = {
    val treeExpandedIcon  = UIUtil.getTreeExpandedIcon
    val treeCollapsedIcon = UIUtil.getTreeCollapsedIcon
    val width             = math.max(treeExpandedIcon.getIconWidth, treeCollapsedIcon.getIconWidth)
    var icon              = if myExpanded.get() then treeExpandedIcon else treeCollapsedIcon
    val extraSpace        = width - icon.getIconWidth
    if extraSpace > 0 then
      val left = extraSpace / 2
      icon = IndentedIcon(icon, Insets(0, left, 0, extraSpace - left))
    myLabel.setIcon(icon)
    myLabel.setDisabledIcon(IconLoader.getTransparentIcon(icon, 0.5f))
  }
}

