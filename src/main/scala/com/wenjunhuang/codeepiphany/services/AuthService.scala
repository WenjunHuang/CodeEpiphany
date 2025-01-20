package com.wenjunhuang.codeepiphany.services

import cats.effect.kernel.Async
import cats.syntax.all.*
import java.net.HttpCookie
import java.util.concurrent.atomic.AtomicReference

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project

import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, SensitiveDataStore }
import com.wenjunhuang.codeepiphany.model.CodeDojo.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.services.login.LoginDialog
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.implicits.*

@Service(Array(Level.PROJECT))
final class AuthService(private val myProject: Project) {
  private val myLoginCache = AtomicReference[Set[CodeDojo]](Set.empty)

  def askForLogout[F[_]: Async: HttpClientManager](codeDojo: CodeDojo): F[Unit] =
    HttpClientManager[F].clearCookiesForHost(codeDojo.domain) *> Async[F].unit

  def validateUserCookieAndTestLogin[F[_]: Async: HttpClientManager](codeDojo: CodeDojo, cookie: String): F[Boolean] =
    Async[F]
      .delay(CookieUtil.parseCookies(cookie))
      .flatMap(validateUserCookieAndTestLogin(codeDojo, _))

  def validateUserCookieAndTestLogin[F[_]: Async: HttpClientManager](
    codeDojo: CodeDojo,
    cookies: List[HttpCookie]
  ): F[Boolean] =
    HttpClientManager[F].updateCookiesForHost(codeDojo.domain, cookies) *>
      checkLoginStatus(codeDojo).flatMap {
        case true  => saveAuthentication(codeDojo, cookies) *> true.pure[F]
        case false => HttpClientManager[F].clearCookiesForHost(codeDojo.domain) *> false.pure[F]
      }

  def loadAuthenticationMayAskForLogin[F[_]: Async: HttpClientManager](codeDojo: CodeDojo): F[AskForLoginResult] =
    for
      _           <- loadAuthentication(codeDojo)
      r           <- isAuthenticated(codeDojo)
      loginResult <- if !r then askForLogin(codeDojo) else Async[F].delay(AskForLoginResult.Done)
    yield loginResult

  def isLoggedIn(codeDojo: CodeDojo): Boolean = myLoginCache.get().contains(codeDojo)

  def setLogin(codeDojo: CodeDojo): Unit = myLoginCache.updateAndGet(_ + codeDojo)

  def clearLogin(codeDojo: CodeDojo): Unit = myLoginCache.updateAndGet(_ - codeDojo)

  def isAuthenticated[F[_]: Async: HttpClientManager](codeDojo: CodeDojo): F[Boolean] =
    checkLoginStatus(codeDojo)

  private def loadAuthentication[F[_]: Async: HttpClientManager](codeDojo: CodeDojo): F[Unit] =
    Async[F].blocking(SensitiveDataStore.loadData(codeDojo.value)).flatMap {
      case Some(authCookies) =>
        Async[F]
          .delay(CookieUtil.parseCookies(authCookies))
          .flatMap(HttpClientManager[F].updateCookiesForHost(codeDojo.domain, _))
      case None => ().pure[F]
    }

  private def saveAuthentication[F[_]: Async: HttpClientManager](
    codeDojo: CodeDojo,
    authCookies: List[HttpCookie]
  ): F[Unit] =
    Async[F].delay {
      SensitiveDataStore.saveData(
        codeDojo.value,
        authCookies.map(cookie => s"${cookie.getName}=${cookie.getValue}").mkString(";")
      )
    }

  private def askForLogin[F[_]: Async](codeDojo: CodeDojo): F[AskForLoginResult] =
    Async[F]
      .async_[AskForLoginResult] { cb =>
        val dialog = new LoginDialog(myProject, codeDojo, cb)
        dialog.show()
      }
      .evalOnEDTAny()

  private def checkLoginStatus[F[_]: Async: HttpClientManager](codeDojo: CodeDojo): F[Boolean] =
    codeDojo match
      case CodeDojo.HackerRank => HackerRankApi[F]().checkLogin()
      case CodeDojo.LeetCode   => LeetCodeApi[F](LeetCode).checkLogin()
      case CodeDojo.LeetCodeCN => LeetCodeApi[F](LeetCodeCN).checkLogin()
}

object AuthService {
  def getInstance(project: Project): AuthService = project.getService(classOf[AuthService])
}
