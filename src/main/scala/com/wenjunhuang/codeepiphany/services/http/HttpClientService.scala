package com.wenjunhuang.codeepiphany.services.http

import cats.effect.{IO, Resource}
import org.http4s.client.Client

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.utils.syntax.*

@Service
final class HttpClientService(private val project: Project) {
  implicit val httpClientManager: HttpClientManager[IO] = HttpClientManager.make[IO]()
  implicit val http4sClient: Resource[IO, Client[IO]] = httpClientManager.getClient
}

object HttpClientService {
  def getInstance(project: Project): HttpClientService = project.getService(classOf[HttpClientService])

  implicit def http4sClient(implicit project: Project): Resource[IO, Client[IO]] = getInstance(project).http4sClient
}
