package com.wenjunhuang.codeepiphany.utils

import cats.effect.{ IO, Resource }
import cats.syntax.all.*
import com.sun.net.httpserver.{ HttpExchange, HttpHandler, HttpServer }
import java.io.{ IOException, InputStream }
import java.net.{ InetSocketAddress, URL }
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.duration.*

import com.wenjunhuang.codeepiphany.utils.syntax.*

object ResourceHttpServer {
  def apply(resourcePath: String, port: Int): Resource[IO, ResourceHttpServer] =
    Resource.make(IO(new ResourceHttpServer(resourcePath, port)).flatTap(_.start))(server => server.stop)
}

class ResourceHttpServer private (private val myResourcePath: String, private val myPort: Int) {
  private val myLogger                   = LoggerFactory.getLogger[IO]
  private var server: Option[HttpServer] = None

  def getResourcePath: String = myResourcePath

  def getListeningPort: Option[Int] = server.map(_.getAddress.getPort)

  private class FileHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      try {
        val requestPath = exchange.getRequestURI.getPath
        // 移除开头的斜杠，因为getResourceAsStream不需要开头的斜杠
        val resourcePath = if (requestPath.startsWith("/")) {
          requestPath.substring(1)
        } else {
          requestPath
        }

        val fullResourcePath = if (myResourcePath.endsWith("/")) {
          myResourcePath + resourcePath
        } else {
          myResourcePath + "/" + resourcePath
        }

        // 尝试获取资源URL
        Option(getClass.getClassLoader.getResource(fullResourcePath)) match {
          case None =>
            sendError(exchange, 404, "Resource not found")
          case Some(url) =>
            handleResource(exchange, url)
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          sendError(exchange, 500, "Internal Server Error")
      } finally {
        exchange.close()
      }
    }

    private def handleResource(exchange: HttpExchange, resourceUrl: URL): Unit = {
      val connection    = resourceUrl.openConnection()
      val contentLength = connection.getContentLength
      val contentType   = connection.getContentType

      exchange.getResponseHeaders.set("Content-Type", Option(contentType).getOrElse("application/octet-stream"))

      if (contentLength >= 0) {
        exchange.sendResponseHeaders(200, contentLength)
      } else {
        // 如果无法确定内容长度，使用分块传输
        exchange.sendResponseHeaders(200, 0)
      }

      val inputStream = connection.getInputStream
      try {
        val os        = exchange.getResponseBody
        val buffer    = new Array[Byte](4096)
        var bytesRead = inputStream.read(buffer)
        while (bytesRead != -1) {
          os.write(buffer, 0, bytesRead)
          bytesRead = inputStream.read(buffer)
        }
        os.close()
      } finally {
        inputStream.close()
      }
    }

    private def sendError(exchange: HttpExchange, code: Int, message: String): Unit = {
      val response = message.getBytes("UTF-8")
      exchange.getResponseHeaders.set("Content-Type", "text/plain; charset=UTF-8")
      exchange.sendResponseHeaders(code, response.length)
      exchange.getResponseBody.write(response)
      exchange.getResponseBody.close()
    }
  }

  private def start: IO[Unit] = IO.blocking {
    val newServer = HttpServer.create(new InetSocketAddress("127.0.0.1", myPort), 0)
    newServer.createContext("/", new FileHandler)
    newServer.setExecutor(intellijComputeContext) // 使用默认执行器
    newServer.start()
    server = Some(newServer)
  } >> myLogger.debug(s"HTTP server started at http://localhost:${getListeningPort.getOrElse(myPort)}/")

  private def stop: IO[Unit] = IO.blocking {
    server.foreach { s =>
      s.stop(0) // 立即停止
    }
    server = None
  } >> myLogger.debug("HTTP server stopped")
}
