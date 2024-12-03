package com.wenjunhuang.codeepiphany.controllers.http

import cats.effect.{ IO, Resource }
import com.intellij.openapi.project.Project
import org.http4s.client.Client

class HttpClientService(private val project: Project) {
  val client: Resource[IO, Client[IO]] = HttpClientKeeper[IO]().getClient
}

object HttpClientService {
  def getInstance(project:Project):HttpClientService = project.getService(classOf[HttpClientService])
}
