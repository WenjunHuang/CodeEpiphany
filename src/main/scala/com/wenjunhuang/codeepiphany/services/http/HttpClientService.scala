package com.wenjunhuang.codeepiphany.services.http

import cats.effect.{IO, Resource}
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.utils.implicits.*
import org.http4s.client.Client

@Service
final class HttpClientService(private val project: Project) {
  implicit val httpClientKeeper: HttpClientKeeper[IO] = HttpClientKeeper.make[IO]()
  implicit val http4sClient: Resource[IO, Client[IO]] = httpClientKeeper.getClient
}

object HttpClientService {
  def getInstance(project: Project): HttpClientService = project.getService(classOf[HttpClientService])

  implicit def http4sClient(implicit project: Project): Resource[IO, Client[IO]] = getInstance(project).http4sClient
}
