package com.wenjunhuang.codeepiphany.utils

import java.awt.Graphics2D
import scala.util.Using.Releasable

trait SwingOps {
  implicit val graphics2DReleasable: Releasable[Graphics2D] = new Releasable[java.awt.Graphics2D] {
    override def release(resource: Graphics2D): Unit = resource.dispose()
  }
}
