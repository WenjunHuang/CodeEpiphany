package com.wenjunhuang.codeepiphany.utils
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.services.http.{ HttpClientManager, HttpClientService }
import cats.effect.IO

trait ServiceOps {
  implicit def httpClientKeeper(implicit project: Project): HttpClientManager[IO] =
    HttpClientService.getInstance(project).httpClientManager
}
