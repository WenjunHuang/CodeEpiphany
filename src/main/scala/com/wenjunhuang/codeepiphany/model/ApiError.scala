package com.wenjunhuang.codeepiphany.model

import scala.util.control.NoStackTrace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

import com.wenjunhuang.codeepiphany.model.Constants.PROJECT_ID

enum ApiError extends NoStackTrace {
  case NotFound(dojo: CodeDojo, message: String) extends ApiError

  case BadRequest(dojo: CodeDojo, message: String) extends ApiError

  case Unauthorized(dojo: CodeDojo, message: String) extends ApiError

  case Conflict(dojo: CodeDojo, message: String) extends ApiError

  case InvalidContent(dojo: CodeDojo, message: String) extends ApiError

  override def toString: String = this match
    case NotFound(dojo, message)       => s"CodeDojo:${dojo.value} NotFound: $message"
    case BadRequest(dojo, message)     => s"CodeDojo:${dojo.value} BadRequest: $message"
    case Unauthorized(dojo, message)   => s"CodeDojo:${dojo.value} Unauthorized: $message"
    case Conflict(dojo, message)       => s"CodeDojo:${dojo.value} Conflict: $message"
    case InvalidContent(dojo, message) => s"CodeDojo:${dojo.value} InvalidContent: $message"
}

trait DojoLoginNotifier {
  def login(project: Project): Unit
  def logout(project: Project): Unit
  def loginExpired(project: Project): Unit
}

object DojoLoginNotifier {

  final val HACKERRANK_LOGIN_LOGOUT_TOPIC: String = PROJECT_ID + ".hackerrank.login_logout.topic"

  @Topic.AppLevel
  val HackerRankLoginTopic = new Topic(HACKERRANK_LOGIN_LOGOUT_TOPIC, classOf[DojoLoginNotifier])

  def getLoginTopic(dojo: CodeDojo): Option[Topic[DojoLoginNotifier]] = dojo match
    case CodeDojo.HackerRank => Some(HackerRankLoginTopic)
    case _                   => None
}

extension (error: ApiError) {
  def publishEvent(project: Project): Unit =
    error match
      case ApiError.Unauthorized(dojo, _) =>
        DojoLoginNotifier
          .getLoginTopic(dojo)
          .foreach(ApplicationManager.getApplication.getMessageBus.syncPublisher(_).loginExpired(project))
      case _ =>
}
