package com.wenjunhuang.codeepiphany.utils

import cats.effect.std.Queue
import cats.effect.{ IO, Resource }
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.{ HttpExchange, HttpHandler, HttpServer }
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.utils.implicits.*
import fs2.Stream
import io.circe.generic.auto.*
import io.circe.parser.decode
import cats.syntax.all.*

import scala.io.Source
package object competitiveCompanion {

  final val defaultPorts = List(1327, 4244, 6174, 10042, 10043, 10045, 27121)

  case class CompetitiveProblem(name: String, group: String, url: String)

  def startCCListening(project: Project, codeDojo: CodeDojo, port: Int): Stream[IO, CompetitiveProblem] = {
    Stream
      .eval(Queue.unbounded[IO, CompetitiveProblem])
      .flatMap { queue =>
        Stream.resource {
          Resource.make(IO.delay {
            val server = HttpServer.create(new java.net.InetSocketAddress("0.0.0.0", port), 0)
            server.createContext(
              "/",
              (exchange: HttpExchange) => {
                if ("POST".equals(exchange.getRequestMethod)) {
                  val body = Source.fromInputStream(exchange.getRequestBody).mkString
                  decode[CompetitiveProblem](body) match {
                    case Right(problem) =>
                      queue.offer(problem).unsafeRunSync()
                      exchange.sendResponseHeaders(200, 0)
                      exchange.getResponseBody.close()

                    case Left(_) =>
                      exchange.sendResponseHeaders(400, 0)
                      exchange.getResponseBody.close()
                  }
                } else {
                  exchange.sendResponseHeaders(405, 0)
                  exchange.getResponseBody.close()
                }
              }
            )
            server.start()
            server
          })(server => IO.delay { server.stop(0) })
        }.map(_ => queue)
      }
      .evalTap { _ =>
        console.info[IO](
          project,
          s"Competitive Companion server started on port $port. Listening for ${codeDojo.show} problem creation..."
        )
      }
      .onFinalize(
        console.info[IO](project, s"Competitive Companion server on port $port for ${codeDojo.show} stopped.")
      )
      .flatMap { queue =>
        Stream.fromQueueUnterminated(queue)
      }
  }
}
