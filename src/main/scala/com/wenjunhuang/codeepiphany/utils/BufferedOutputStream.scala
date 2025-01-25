package com.wenjunhuang.codeepiphany.utils

import java.io.{ByteArrayOutputStream, OutputStream}

class BufferedOutputStream(private val myOnFlush: (Array[Byte]) => Unit) extends OutputStream {
  private val myBuffer = ByteArrayOutputStream()

  override def write(b: Int): Unit =
    myBuffer.write(b)

  override def flush(): Unit = {
    val data = myBuffer.toByteArray
    myOnFlush(data)
  }

  override def close(): Unit = {
    flush()
    myBuffer.close()
  }
}
