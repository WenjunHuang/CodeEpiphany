package com.wenjunhuang.codeepiphany.utils.ui

import java.awt.{Color, Component, Graphics, Graphics2D}

import com.intellij.util.ui.{EmptyIcon, GraphicsUtil}

class PartialFillIcon(width:Int,height:Int,private val myColor:Color) extends EmptyIcon(width,height) {
  override def paintIcon(component: Component, g: Graphics, i: Int, j: Int): Unit = {

  }
}
