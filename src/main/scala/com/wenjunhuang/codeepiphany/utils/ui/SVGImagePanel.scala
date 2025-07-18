package com.wenjunhuang.codeepiphany.utils.ui

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.parser.SVGLoader
import com.github.weisj.jsvg.view.{FloatSize, ViewBox}
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.GraphicsUtil

import java.awt.{Graphics, Graphics2D}
import scala.compiletime.uninitialized

class SVGImagePanel extends JBPanel[SVGImagePanel](true) {
  private var mySvgDocument: SVGDocument = uninitialized

  def image_=(image: Option[String]): Unit = {
    image match
      case Some(image) =>
        mySvgDocument = SVGLoader().load(getClass.getResource(image))
      case None =>
        mySvgDocument = null
    repaint()
  }

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)
    if mySvgDocument != null then
      val config = GraphicsUtil.setupAAPainting(g)
      mySvgDocument.render(
        this,
        g.asInstanceOf[Graphics2D],
        ViewBox(FloatSize(getWidth.toFloat, getHeight.toFloat))
      )
      config.restore()
  }
}
