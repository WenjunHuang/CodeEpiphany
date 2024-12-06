package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.wenjunhuang.codeepiphany.utils.intellijUIContext
import com.wenjunhuang.codeepiphany.controllers.http.{ HttpClientKeeper, HttpClientService }
import com.wenjunhuang.codeepiphany.hackerrank.HackerRankApi
import com.wenjunhuang.codeepiphany.hackerrank.services.auth.ui.HackerRankLoginDialog
import com.wenjunhuang.codeepiphany.model.{ ApiError, CodeDojo }
import com.wenjunhuang.codeepiphany.utils.SensitiveDataStore
import org.apache.http.client.utils.HttpClientUtils
import org.http4s.*
import org.http4s.dsl.io.*

import scala.jdk.CollectionConverters.*
import java.net.HttpCookie

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
          .delay(HttpCookie.parse(authCookies).asScala.toList)
          .flatMap(it => HttpClientKeeper[F].updateCookies(codeDojo.host, it))
      case None => ().pure[F]
    }

  def saveAuthentication[F[_]: Async: HttpClientKeeper](project: Project, codeDojo: CodeDojo, authCookies: List[HttpCookie]): F[Unit] =
    Async[F].delay {
      SensitiveDataStore.saveData(codeDojo.show, authCookies.map(cookie => s"${cookie.getName}=${cookie.getValue}").mkString(";"))
    }

  /** Invoke the login process * */
  def askForLogin[F[_]: Async](project: Project, codeDojo: CodeDojo): F[AskForLoginResult] =
    Async[F].evalOn(
      Async[F].async_ { cb =>
        val dialog = new HackerRankLoginDialog(project, cb)
        dialog.show()
      },
      intellijUIContext
    )

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
