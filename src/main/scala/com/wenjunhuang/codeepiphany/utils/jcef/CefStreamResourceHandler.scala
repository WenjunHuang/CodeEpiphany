package com.wenjunhuang.codeepiphany.utils.jcef

import cats.effect.SyncIO
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.misc.{IntRef, StringRef}
import org.cef.network.{CefRequest, CefResponse}

import java.io.InputStream

class CefStreamResourceHandler(private val myStream: InputStream, private val myMimeType: String, parent: Disposable, private val headers: Map[String, String] = Map.empty)
    extends CefResourceHandler,
      Disposable {
  Disposer.register(parent, this)

  override def processRequest(request: CefRequest, callback: CefCallback): Boolean = {
    callback.Continue()
    true
  }

  override def getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef): Unit = {
    response.setMimeType(myMimeType)
    response.setStatus(200)
    for ((k, v) <- headers)
      response.setHeaderByName(k, v, true)
  }

  override def readResponse(dataOut: Array[Byte], bytesToRead: Int, bytesRead: IntRef, callback: CefCallback): Boolean =
    SyncIO.delay {
      bytesRead.set(myStream.read(dataOut, 0, bytesToRead))
      bytesRead.get()
    }.handleErrorWith(_ => SyncIO.pure(-1))
      .map { result =>
        if result == -1 then
          bytesRead.set(0)
          Disposer.dispose(this)
          false
        else true
      }
      .unsafeRunSync()

  override def cancel(): Unit =
    Disposer.dispose(this)

  override def dispose(): Unit =
    SyncIO
      .delay(myStream.close())
      .handleErrorWith(e => SyncIO.delay(Logger.getInstance(classOf[CefStreamResourceHandler]).warn("Failed to close the stream", e)))
      .unsafeRunSync()
}
