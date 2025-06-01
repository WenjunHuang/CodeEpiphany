package com.wenjunhuang.codeepiphany.utils

import java.awt.{ Graphics2D, Image, RenderingHints }
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.Icon

import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.ui.{ AvatarIcon, AvatarPalette, ImageUtil }

class AsyncAvatarLoader(username: String, avatarUrl: String, size: Int) extends Icon {
  @volatile
  private var image: Option[Image]        = None
  private var listeners: List[() => Unit] = Nil
  private val placeholder = new AvatarIcon(size, 1.0, username, username.charAt(0).toString, AvatarPalette.INSTANCE)

  ApplicationManagerEx.getApplicationEx.executeOnPooledThread(new Runnable {
    override def run(): Unit = {
      try {
        val url = new URL(avatarUrl)
        val img = ImageIO.read(url)
        if (img != null) {
          val scaled   = ImageUtil.createCircleImage(img).getScaledInstance(size, size, Image.SCALE_SMOOTH)
          val buffered = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
          val g        = buffered.createGraphics()
          ImageUtil.applyQualityRenderingHints(g)
          g.drawImage(scaled, 0, 0, null)
          g.dispose()
          image = Some(buffered)
          ApplicationManagerEx.getApplicationEx.invokeLater(() => listeners.foreach(_()))
        }
      } catch {
        case _: Exception => // 使用占位符
      }
    }
  })

  def addListener(listener: () => Unit): Unit = listeners = listener :: listeners

  override def paintIcon(c: java.awt.Component, g: java.awt.Graphics, x: Int, y: Int): Unit = {
    image match {
      case Some(img) =>
        g.drawImage(img, x, y, null)
      case None => placeholder.paintIcon(c, g, x, y)
    }
  }

  override def getIconWidth: Int = size

  override def getIconHeight: Int = size
}
