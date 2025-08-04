package com.wenjunhuang.codeepiphany.utils

import cats.effect.{ IO, Resource }
import com.sun.net.httpserver.{ HttpExchange, HttpHandler, HttpServer }
import java.net.{ InetSocketAddress, URL }
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils
import org.typelevel.log4cats.LoggerFactory
import scala.collection.mutable
import scala.util.Using

import com.wenjunhuang.codeepiphany.utils.syntax.*

class ResourceHttpServer(private val myRootResourcePath: String, private val myPort: Int) {
  private val myLogger = LoggerFactory.getLogger[IO]

  @volatile
  private var server: Option[HttpServer] = None

  private case class CustomResponse(content: Array[Byte], contentType: String)
  private case class DynamicResponse(generator: () => Array[Byte], contentType: String)

  private val customResponses: mutable.Map[String, CustomResponse]   = mutable.Map.empty
  private val dynamicResponses: mutable.Map[String, DynamicResponse] = mutable.Map.empty

  def getResourcePath: String = myRootResourcePath

  def getListeningPort: Option[Int] = server.map(_.getAddress.getPort)

  def addTemplateResponse(
    path: String,
    contentTemplatePath: String,
    contentType: String,
    variableProvider: () => Map[String, String | (() => String)] = () => Map.empty
  ): Unit = {
    val templateUrl = getClass.getClassLoader.getResource(s"$myRootResourcePath/$contentTemplatePath")
    if (templateUrl == null) {
      myLogger.error(s"Template not found: $contentTemplatePath").unsafeRunSync()
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

  def addCustomResponse(path: String, content: Array[Byte], contentType: String): Unit = {
    customResponses += (path -> CustomResponse(content, contentType))
  }

  def addCustomResponse(path: String, contentGenerator: () => Array[Byte], contentType: String): Unit = {
    dynamicResponses += (path -> DynamicResponse(contentGenerator, contentType))
  }

  private class FileHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      Resource
        .fromAutoCloseable(IO.pure(exchange))
        .use { exchange =>
          IO.delay {
            val requestPath = exchange.getRequestURI.getPath
            // 首先检查动态响应
            dynamicResponses.get(requestPath) match {
              case Some(response) =>
                // 使用动态生成的内容
                val content = response.generator()
                exchange.getResponseHeaders.set("Content-Type", response.contentType)
                exchange.sendResponseHeaders(200, content.length)
                exchange.getResponseBody.write(content)
              case None =>
                // 然后检查静态响应
                customResponses.get(requestPath) match {
                  case Some(response) =>
                    // 使用静态响应
                    exchange.getResponseHeaders.set("Content-Type", response.contentType)
                    exchange.sendResponseHeaders(200, response.content.length)
                    exchange.getResponseBody.write(response.content)
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
          }.handleErrorWith { e =>
            myLogger.error(e)(s"Error handling request for ${exchange.getRequestURI}") *> IO.delay {
              sendError(exchange, 500, "Internal Server Error")
            }
          }
        }
        .unsafeRunSync()
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

      Using.resources(connection.getInputStream, exchange.getResponseBody) { (in, out) =>
        IOUtils.copy(in, out, 4096)
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
