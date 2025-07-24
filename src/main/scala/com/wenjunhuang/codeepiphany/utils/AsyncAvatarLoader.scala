package com.wenjunhuang.codeepiphany.utils

import cats.effect.IO
import com.intellij.util.ui.{ AvatarIcon, ColorPalette, ImageUtil }
import com.wenjunhuang.codeepiphany.utils.syntax.*
import kotlin.Pair as ktPair

import java.awt.image.BufferedImage
import java.awt.{ Color, Image }
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.Icon

class AsyncAvatarLoader(username: String, avatarUrl: String, size: Int) extends Icon {
  @volatile
  private var image: Option[Image]         = None
  private var listener: Option[() => Unit] = None
  private val placeholder =
    new AvatarIcon(size, 1.0, username, username.charAt(0).toString, AsyncAvatarLoader.AvatarPalette)

  IO.delay {
    val url = new URL(avatarUrl)
    val img = ImageIO.read(url)
    if (img != null) {
      val scaled   = ImageUtil.createCircleImage(img).getScaledInstance(size, size, Image.SCALE_SMOOTH)
      val buffered = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
      val g        = buffered.createGraphics()
      ImageUtil.applyQualityRenderingHints(g)
      g.drawImage(scaled, 0, 0, null)
      g.dispose()
      Some(buffered)
    } else {
      None
    }
  }.flatMap {
    case None => IO.unit
    case Some(buffered) =>
      IO.delay {
        image = Some(buffered)
        listener.foreach(_())
      }.evalOnEDTAny()
  }.unsafeRunAndForget()

  def setListener(newListener: () => Unit): Unit = {
    listener = Some(newListener)
  }

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

object AsyncAvatarLoader {
  private object AvatarPalette extends ColorPalette {
    override def getGradients: Array[ktPair[Color, Color]] =
      Array(
        new ktPair(Color(0x60a800), Color(0xd5ca00)),
        ktPair(Color(0x0a81f6), Color(0x0a81f6)),
        ktPair(Color(0xab3af2), Color(0xe40568)),
        ktPair(Color(0x21d370), Color(0x03e9e1)),
        ktPair(Color(0x765af8), Color(0x5a91f8)),
        ktPair(Color(0x9f2aff), Color(0xe9a80b)),
        ktPair(Color(0x3ba1ff), Color(0x36e97d)),
        ktPair(Color(0x9e54ff), Color(0x0acff6)),
        ktPair(Color(0xd50f6b), Color(0xe73ae8)),
        ktPair(Color(0x00c243), Color(0x00ffff)),
        ktPair(Color(0xb345f1), Color(0x669dff)),
        ktPair(Color(0xed5502), Color(0xe73ae8)),
        ktPair(Color(0x4be098), Color(0x627fff)),
        ktPair(Color(0x765af8), Color(0xc059ee)),
        ktPair(Color(0xed358c), Color(0xdbed18)),
        ktPair(Color(0x168bfa), Color(0x26f7c7)),
        ktPair(Color(0x9039d0), Color(0xc239d0)),
        ktPair(Color(0xed358c), Color(0xf9902e)),
        ktPair(Color(0x9d4cff), Color(0x39d3c3)),
        ktPair(Color(0x9f2aff), Color(0xfd56fd)),
        ktPair(Color(0xff7500), Color(0xffca00))
      )
  }
}
