package com.wenjunhuang.codeepiphany.utils

import cats.effect.IO
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import com.wenjunhuang.codeepiphany.utils.syntax.*
import org.typelevel.log4cats.LoggerFactory

import java.net.{InetSocketAddress, URL}
import java.nio.charset.StandardCharsets
import scala.collection.mutable

class ResourceHttpServer(private val myRootResourcePath: String, private val myPort: Int) {
  private val myLogger                   = LoggerFactory.getLogger[IO]
  private var server: Option[HttpServer] = None

  private case class CustomResponse(content: Array[Byte], contentType: String)
  private case class DynamicResponse(generator: () => Array[Byte], contentType: String)
  private val customResponses: mutable.Map[String, CustomResponse]   = mutable.Map.empty
  private val dynamicResponses: mutable.Map[String, DynamicResponse] = mutable.Map.empty

  def getResourcePath: String = myRootResourcePath

  def getListeningPort: Option[Int] = server.map(_.getAddress.getPort)

  def addTemplateResponse(
    path: String,
    templatePath: String,
    contentType: String,
    variableProvider: () => Map[String, String | (() => String)] = () => Map.empty
  ): Unit = {
    val templateUrl = getClass.getClassLoader.getResource(s"$myRootResourcePath/$templatePath")
    if (templateUrl == null) {
      myLogger.error(s"Template not found: $templatePath").unsafeRunSync()
      return
    }

    addCustomResponse(
      path,
      () => {
        val template = new String(templateUrl.openStream().readAllBytes(), StandardCharsets.UTF_8)
        val processedContent = variableProvider().foldLeft(template) { case (acc, (key, value)) =>
          value match {
            case str: String          => acc.replace(key, str)
            case func: (() => String) => acc.replace(key, func())
          }
        }
        processedContent.getBytes(StandardCharsets.UTF_8)
      },
      contentType
    )
  }

  def addCustomResponse(
    path: String,
    content: String,
    contentType: String,
    variableProvider: () => Map[String, String] = () => Map.empty
  ): Unit = {
    addCustomResponse(
      path,
      () => {
        val processedContent = variableProvider().foldLeft(content) { case (acc, (key, value)) =>
          acc.replace(key, value)
        }
        processedContent.getBytes("UTF-8")
      },
      contentType
    )
  }

  def addCustomResponse(path: String, content: Array[Byte], contentType: String): Unit = {
    customResponses += (path -> CustomResponse(content, contentType))
  }

  def addCustomResponse(path: String, generator: () => Array[Byte], contentType: String): Unit = {
    dynamicResponses += (path -> DynamicResponse(generator, contentType))
  }

  private class FileHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      try {
        val requestPath = exchange.getRequestURI.getPath

        // 首先检查动态响应
        dynamicResponses.get(requestPath) match {
          case Some(response) =>
            // 使用动态生成的内容
            val content = response.generator()
            exchange.getResponseHeaders.set("Content-Type", response.contentType)
            exchange.sendResponseHeaders(200, content.length)
            exchange.getResponseBody.write(content)
            exchange.getResponseBody.close()
          case None =>
            // 然后检查静态响应
            customResponses.get(requestPath) match {
              case Some(response) =>
                // 使用静态响应
                exchange.getResponseHeaders.set("Content-Type", response.contentType)
                exchange.sendResponseHeaders(200, response.content.length)
                exchange.getResponseBody.write(response.content)
                exchange.getResponseBody.close()
              case None =>
                // 使用默认的资源处理逻辑
                val resourcePath = if (requestPath.startsWith("/")) {
                  requestPath.substring(1)
                } else {
                  requestPath
                }

                val fullResourcePath = if (myRootResourcePath.endsWith("/")) {
                  myRootResourcePath + resourcePath
                } else {
                  myRootResourcePath + "/" + resourcePath
                }

                Option(getClass.getClassLoader.getResource(fullResourcePath)) match {
                  case None =>
                    sendError(exchange, 404, "Resource not found")
                  case Some(url) =>
                    handleResource(exchange, url)
                }
            }
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
      val contentType = resourceUrl.getPath match {
        case path if path.endsWith(".html")                         => "text/html"
        case path if path.endsWith(".css")                          => "text/css"
        case path if path.endsWith(".js")                           => "application/javascript"
        case path if path.endsWith(".json")                         => "application/json"
        case path if path.endsWith(".png")                          => "image/png"
        case path if path.endsWith(".jpg") | path.endsWith(".jpeg") => "image/jpeg"
        case path if path.endsWith(".gif")                          => "image/gif"
        case path if path.endsWith(".svg")                          => "image/svg+xml"
        case path if path.endsWith(".ico")                          => "image/x-icon"
        case path if path.endsWith(".ttf")                          => "font/ttf"
        case path if path.endsWith(".woff")                         => "font/woff"
        case path if path.endsWith(".woff2")                        => "font/woff2"
        case path if path.endsWith(".eot")                          => "application/vnd.ms-fontobject"
        case path if path.endsWith(".otf")                          => "font/otf"
        case path if path.endsWith(".xml")                          => "application/xml"
        case path if path.endsWith(".txt")                          => "text/plain"
        case path if path.endsWith(".md")                           => "text/markdown"
        case path if path.endsWith(".pdf")                          => "application/pdf"
        case path if path.endsWith(".zip")                          => "application/zip"
        case path if path.endsWith(".tar")                          => "application/x-tar"
        case path if path.endsWith(".gz")                           => "application/gzip"
        case _                                                      => "application/octet-stream"
      }

      exchange.getResponseHeaders.set("Content-Type", contentType)

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

  def start(): Unit = {
    val newServer = HttpServer.create(new InetSocketAddress("127.0.0.1", myPort), 0)
    newServer.createContext("/", new FileHandler)
    newServer.setExecutor(intellijComputeContext) // 使用默认执行器
    newServer.start()
    server = Some(newServer)
  }

  def stop(): Unit = {
    server.foreach { s =>
      s.stop(0) // 立即停止
    }
    server = None
  }
}
