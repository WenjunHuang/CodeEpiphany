package com.wenjunhuang.codeepiphany.utils.ui

import com.intellij.util.ui.GraphicsUtil
import com.wenjunhuang.codeepiphany.utils.syntax.*

import java.awt.{Graphics, Graphics2D, LayoutManager}
import javax.swing.JComponent
import scala.util.Using

class BackgroundRoundedPanel(private val radius: Int, private val layoutManager: LayoutManager) extends JComponent {

  setLayout(layoutManager)
  setOpaque(false)

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)
    Using.resource(g.create().asInstanceOf[Graphics2D]) { g2 =>
      val config = GraphicsUtil.setupAAPainting(g2)
      g2.setColor(getBackground)
      g2.fillRoundRect(0, 0, getWidth, getHeight, radius, radius)
      config.restore()
    }
  }
}
