package com.wenjunhuang.codeepiphany.utils
import cats.effect.IO

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}

trait ServiceOps {
  implicit def httpClientKeeper(implicit project: Project): HttpClientManager[IO] =
    HttpClientService.getInstance(project).httpClientManager
}
