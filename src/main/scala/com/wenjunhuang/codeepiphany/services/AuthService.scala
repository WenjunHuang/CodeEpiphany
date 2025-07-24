package com.wenjunhuang.codeepiphany.services

import cats.effect.IO
import cats.effect.implicits.*
import cats.syntax.all.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.atcoder.services.AtCoderApi
import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesApi
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.leetcode.services.LeetCodeApi
import com.wenjunhuang.codeepiphany.luogu.services.LuoGuApi
import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.model.CodeDojo.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.services.login.LoginDialog
import com.wenjunhuang.codeepiphany.utils.CookieUtil
import com.wenjunhuang.codeepiphany.utils.syntax.*

import java.net.HttpCookie
import java.util.concurrent.atomic.AtomicReference

@Service(Array(Level.PROJECT))
final class AuthService(private val myProject: Project) {
  private val myLoginCache = AtomicReference[Set[CodeDojo]](Set.empty)

  def askForLogout(codeDojo: CodeDojo): IO[Unit] =
    (removeAuthentication(codeDojo), HttpClientManager.clearCookiesForHost(codeDojo.domain)).parTupled.void

  def validateUserCookieAndTestLogin(codeDojo: CodeDojo, cookie: String): IO[Boolean] =
    IO
      .delay(CookieUtil.parseCookies(cookie))
      .flatMap(validateUserCookieAndTestLogin(codeDojo, _))

  def validateUserCookieAndTestLogin(codeDojo: CodeDojo, cookies: List[HttpCookie]): IO[Boolean] =
    HttpClientManager.updateCookiesForHost(codeDojo.domain, cookies) *>
      checkLoginStatus(codeDojo).flatMap {
        case true  => saveAuthentication(codeDojo, cookies) *> true.pure[IO]
        case false => HttpClientManager.clearCookiesForHost(codeDojo.domain) *> false.pure[IO]
      }

  def loadAuthenticationMayAskForLogin(codeDojo: CodeDojo): IO[AskForLoginResult] =
    for
      _           <- loadAuthentication(codeDojo)
      r           <- isAuthenticated(codeDojo)
      loginResult <- if !r then askForLogin(codeDojo) else IO.delay(AskForLoginResult.Done)
    yield loginResult

  def isLoggedIn(codeDojo: CodeDojo): Boolean = myLoginCache.get().contains(codeDojo)

  def setLogin(codeDojo: CodeDojo): Unit = myLoginCache.updateAndGet(_ + codeDojo)

  def clearLogin(codeDojo: CodeDojo): Unit = myLoginCache.updateAndGet(_ - codeDojo)

  private def isAuthenticated(codeDojo: CodeDojo): IO[Boolean] =
    checkLoginStatus(codeDojo)

  private def loadAuthentication(codeDojo: CodeDojo): IO[Unit] =
    IO.blocking(SensitiveDataStore.loadData(codeDojo.value)).flatMap {
      case Some(authCookies) =>
        IO
          .delay(CookieUtil.parseCookies(authCookies))
          .flatMap(HttpClientManager.updateCookiesForHost(codeDojo.domain, _))
      case None => IO.unit
    }

  private def saveAuthentication(codeDojo: CodeDojo, authCookies: List[HttpCookie]): IO[Unit] =
    IO.blocking {
      SensitiveDataStore.saveData(codeDojo.value, CookieUtil.encodeCookies(authCookies))
    }

  private def removeAuthentication(codeDojo: CodeDojo): IO[Unit] = IO.blocking {
    SensitiveDataStore.removeData(codeDojo.value)
  }

  private def askForLogin(codeDojo: CodeDojo): IO[AskForLoginResult] =
    IO
      .async_[AskForLoginResult] { cb =>
        val dialog = new LoginDialog(myProject, codeDojo, cb)
        dialog.show()
      }
      .evalOnEDTAny()

  private def checkLoginStatus(codeDojo: CodeDojo): IO[Boolean] =
    codeDojo match
      case CodeDojo.HackerRank => HackerRankApi.checkLogin()
      case CodeDojo.LeetCode   => LeetCodeApi(LeetCode).checkLogin()
      case CodeDojo.LeetCodeCN => LeetCodeApi(LeetCodeCN).checkLogin()
      case CodeDojo.CodeForces => CodeForcesApi.checkLogin()
      case CodeDojo.AtCoder    => AtCoderApi.checkLogin()
      case CodeDojo.LuoGu      => LuoGuApi.checkLogin()
}

object AuthService {
  def getInstance(project: Project): AuthService = project.getService(classOf[AuthService])
}
