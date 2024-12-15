package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.controllers.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.ui.HackerRankLoginDialog
import com.wenjunhuang.codeepiphany.model.{ApiError, CodeDojo, SensitiveDataStore}
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*

import java.net.HttpCookie
import scala.jdk.CollectionConverters.*

package object auth {
  enum AskForLoginResult {
    case Done
    case Cancelled
  }

  def isAuthenticated[F[_]: Async: HttpClientKeeper](project: Project, codeDojo: CodeDojo): F[Boolean] =
    codeDojo match
      case CodeDojo.HackerRank => HackerRankApi[F]().checkLogin()
      case _                   => false.pure[F]

  def loadAuthentication[F[_]: Async: HttpClientKeeper](project: Project, codeDojo: CodeDojo): F[Unit] =
    Async[F].blocking(SensitiveDataStore.loadData(codeDojo.show)).flatMap {
      case Some(authCookies) =>
        Async[F]
          .delay(CookieUtil.parseCookies(authCookies))
          .flatMap(it => HttpClientKeeper[F].updateCookiesForHost(codeDojo.domain, it))
      case None => ().pure[F]
    }

  def saveAuthentication[F[_]: Async: HttpClientKeeper](project: Project, codeDojo: CodeDojo, authCookies: List[HttpCookie]): F[Unit] =
    Async[F].delay {
      SensitiveDataStore.saveData(codeDojo.show, authCookies.map(cookie => s"${cookie.getName}=${cookie.getValue}").mkString(";"))
    }

  def validateUserCookieAndTestLogin[F[_]: Async: HttpClientKeeper](project: Project, codeDojo: CodeDojo, cookies: List[HttpCookie]): F[Boolean] =
    HttpClientKeeper[F].updateCookiesForHost(codeDojo.domain, cookies) *> HackerRankApi[F]().checkLogin().flatMap {
      case true =>
        saveAuthentication[F](project, codeDojo, cookies) *> true.pure[F]
      case false => HttpClientKeeper[F].clearCookiesForHost(CodeDojo.HackerRank.domain) *> false.pure[F]
    }

  def validateUserCookieAndTestLogin[F[_]: Async: HttpClientKeeper](project: Project, codeDojo: CodeDojo, cookie: String): F[Boolean] =
    Async[F]
      .delay(CookieUtil.parseCookies(cookie))
      .flatMap(validateUserCookieAndTestLogin(project, codeDojo, _))

  def loadAuthenticationMayAskForLogin[F[_]: Async: HttpClientKeeper](project: Project, codeDojo: CodeDojo): F[AskForLoginResult] =
    for {
      _ <- loadAuthentication(project, codeDojo)
      r <- isAuthenticated(project, codeDojo)
      loginResult <-
        if !r then askForLogin(project, codeDojo)
        else Async[F].delay(AskForLoginResult.Done)
    } yield loginResult

  /** Invoke the login process * */
  def askForLogin[F[_]: Async](project: Project, codeDojo: CodeDojo): F[AskForLoginResult] =
    Async[F].evalOn(
      Async[F].async_ { cb =>
        val dialog = new HackerRankLoginDialog(project, cb)
        dialog.show()
      },
      intellijUIContext
    )

  def askForLogout[F[_]: Async: HttpClientKeeper](project: Project, codeDojo: CodeDojo): F[Unit] =
    HttpClientKeeper[F].clearCookiesForHost(codeDojo.domain) *> Async[F].unit

  extension [F[_], A](effect: F[A]) {

    /** Invoke the login process if an Unauthorized error is thrown. */
    def askForLoginIfUnauthorized(project: Project)(implicit E: Async[F]): F[AskForLoginResult | A] =
      effect.redeemWith(
        recover = {
          case ApiError.Unauthorized(codeDojo, _) => askForLogin(project, codeDojo).map(_.asInstanceOf[AskForLoginResult | A])
          case e                                  => E.raiseError(e)
        },
        bind = E.pure
      )
  }
}
