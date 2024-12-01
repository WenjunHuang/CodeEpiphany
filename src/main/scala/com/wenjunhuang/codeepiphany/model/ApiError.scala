package com.wenjunhuang.codeepiphany.model

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

import scala.util.control.NoStackTrace

enum ApiError extends NoStackTrace {
  case NotFound(dojo: CodeDojo, message: String) extends ApiError

  case BadRequest(dojo: CodeDojo, message: String) extends ApiError

  case Unauthorized(dojo: CodeDojo, message: String) extends ApiError

  case Conflict(dojo: CodeDojo, message: String) extends ApiError

  case InvalidContent(dojo: CodeDojo, message: String) extends ApiError
}

trait DojoLoginNotifier {
  def login(project: Project): Unit
  def logout(project: Project): Unit
  def loginExpired(project: Project): Unit
}

object DojoLoginNotifier {
  @Topic.AppLevel
  val HackerRankLoginTopic = new Topic(Constants.HackerRankLoginTopic, classOf[DojoLoginNotifier])

  def getLoginTopic(dojo: CodeDojo): Option[Topic[DojoLoginNotifier]] = dojo match
    case CodeDojo.HackerRank => Some(HackerRankLoginTopic)
    case _                   => None
}

extension (error: ApiError) {
  def publishEvent(project: Project): Unit =
    error match
      case ApiError.Unauthorized(dojo, _) =>
        DojoLoginNotifier.getLoginTopic(dojo).foreach(ApplicationManager.getApplication.getMessageBus.syncPublisher(_).loginExpired(project))
      case _ =>
}
