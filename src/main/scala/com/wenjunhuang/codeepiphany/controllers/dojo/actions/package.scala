package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.{ Async, IO }
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project

package object actions {
  trait LoginLogoutProvider[F[_]] {
    def login(): F[Unit]
    def logout(): F[Unit]
    def isLoggedIn(): F[Boolean]
  }

  trait ListsQueryParamProvider[F[_]] {
    def getAllItems(): F[List[ListQueryItem]]
    def getSelectedItems(): F[List[ListQueryItem]]
    def addSelectedItems(items: List[ListQueryItem]): F[Unit]
    def removeSelectedItems(items: List[ListQueryItem]): F[Unit]
  }


  case class ListQueryItem(name: String, id: String)

  object keys {
    final val LOGIN_LOGOUT_KEY = DataKey.create[LoginLogoutProvider[IO]]("LOGIN_LOGOUT_KEY")

    final val LISTS_PROVIDER_KEY = DataKey.create[ListsQueryParamProvider[IO]]("LISTS_QUERYPARAM_PROVIDER_KEY")
  }

  object groups {
    final val TOOLBAR_GROUP = "CodeEpiphany.Dojos.Toolbar"
    final val TOOLBAR_PLACE = "CodeEpiphany.Dojos"
  }
}
