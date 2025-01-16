package com.wenjunhuang.codeepiphany.services

import cats.effect.kernel.Async
import cats.syntax.all.*
import java.net.HttpCookie

import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.{ ApiError, CodeDojo, SensitiveDataStore }
import com.wenjunhuang.codeepiphany.model.CodeDojo.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.services.login.LoginDialog
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*

object auth {
  enum AskForLoginResult {
    case Done
    case Cancelled
  }

  def isAuthenticated[F[_]: Async: HttpClientManager](project: Project, codeDojo: CodeDojo): F[Boolean] =
    codeDojo match
      case CodeDojo.HackerRank => HackerRankApi[F]().checkLogin()
      case CodeDojo.LeetCode   => LeetCodeApi[F](LeetCode).checkLogin()
      case CodeDojo.LeetCodeCN => LeetCodeApi[F](LeetCodeCN).checkLogin()
      case _                   => false.pure[F]

  def loadAuthentication[F[_]: Async: HttpClientManager](project: Project, codeDojo: CodeDojo): F[Unit] =
    Async[F].blocking(SensitiveDataStore.loadData(codeDojo.show)).flatMap {
      case Some(authCookies) =>
        Async[F]
          .delay(CookieUtil.parseCookies(authCookies))
          .flatMap(it => HttpClientManager[F].updateCookiesForHost(codeDojo.domain, it))
      case None => ().pure[F]
    }

  def saveAuthentication[F[_]: Async: HttpClientManager](
    project: Project,
    codeDojo: CodeDojo,
    authCookies: List[HttpCookie]
  ): F[Unit] =
    Async[F].delay {
      SensitiveDataStore.saveData(
        codeDojo.show,
        authCookies.map(cookie => s"${cookie.getName}=${cookie.getValue}").mkString(";")
      )
    }

  def validateUserCookieAndTestLogin[F[_]: Async: HttpClientManager](
    project: Project,
    codeDojo: CodeDojo,
    cookies: List[HttpCookie]
  ): F[Boolean] =
    HttpClientManager[F].updateCookiesForHost(codeDojo.domain, cookies)
      *> (codeDojo match
        case HackerRank => HackerRankApi[F]().checkLogin()
        case LeetCode   => LeetCodeApi[F](LeetCode).checkLogin()
        case LeetCodeCN => LeetCodeApi[F](LeetCodeCN).checkLogin()
        case _          => false.pure[F]
      ).flatMap {
        case true =>
          saveAuthentication[F](project, codeDojo, cookies) *> true.pure[F]
        case false => HttpClientManager[F].clearCookiesForHost(codeDojo.domain) *> false.pure[F]
      }

  def validateUserCookieAndTestLogin[F[_]: Async: HttpClientManager](
    project: Project,
    codeDojo: CodeDojo,
    cookie: String
  ): F[Boolean] =
    Async[F]
      .delay(CookieUtil.parseCookies(cookie))
      .flatMap(validateUserCookieAndTestLogin(project, codeDojo, _))

  def loadAuthenticationMayAskForLogin[F[_]: Async: HttpClientManager](
    project: Project,
    codeDojo: CodeDojo
  ): F[AskForLoginResult] =
    for
      _ <- loadAuthentication(project, codeDojo)
      r <- isAuthenticated(project, codeDojo)
      loginResult <-
        if !r then askForLogin(project, codeDojo)
        else Async[F].delay(AskForLoginResult.Done)
    yield loginResult

  /** Invoke the login process * */
  def askForLogin[F[_]: Async](project: Project, codeDojo: CodeDojo): F[AskForLoginResult] =
    Async[F]
      .async_[AskForLoginResult] { cb =>
        val dialog = new LoginDialog(project, codeDojo, cb)
        dialog.show()
      }
      .evalOnEDTAny()

  def askForLogout[F[_]: Async: HttpClientManager](project: Project, codeDojo: CodeDojo): F[Unit] =
    HttpClientManager[F].clearCookiesForHost(codeDojo.domain) *> Async[F].unit

  extension [F[_], A](effect: F[A]) {

    /** Invoke the login process if an Unauthorized error is thrown. */
    def askForLoginIfUnauthorized(project: Project)(implicit E: Async[F]): F[AskForLoginResult | A] =
      effect.redeemWith(
        recover = {
          case ApiError.Unauthorized(codeDojo, _) =>
            askForLogin(project, codeDojo).map(_.asInstanceOf[AskForLoginResult | A])
          case e => E.raiseError(e)
        },
        bind = E.pure
      )
  }
}
