package com.wenjunhuang.codeepiphany.controllers.dojo

import cats.effect.{ Async, IO }
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project

package object actions {
  trait LoginLogoutProvider {
    def login(): Unit
    def logout(): Unit
    def isLoggedIn: Boolean
  }

  trait QueryParamProvider[T] {
    def getAllItems: List[T]
    def isMultipleSelection: Boolean
    def isSelected(item: T): Boolean
    def getSelectedItems: List[T]
    def addSelectedItems(items: List[T]): Unit
    def toggleSelection(item: T): Unit
    def removeSelectedItems(items: List[T]): Unit
  }

  case class ListQueryItem(name: String, id: String)
  trait ListsQueryParamProvider {
    def getAllItems: List[ListQueryItem]
    def isMultipleSelection: Boolean
    def getSelectedItems: List[ListQueryItem]
    def addSelectedItems(items: List[ListQueryItem]): Unit
    def toggleSelection(item: ListQueryItem): Unit
    def removeSelectedItems(items: List[ListQueryItem]): Unit
  }

  case class Difficulty(name: String, value: String)
  trait DifficultiesProvider {
    def getDifficulties: List[Difficulty]
    def isMultipleSelection: Boolean
    def getSelected: List[Difficulty]
    def addSelected(items: List[Difficulty]): Unit
    def toggleSelection(item: Difficulty): Unit
    def removeSelected(items: List[Difficulty]): Unit
    def isSelected(item: Difficulty): Boolean
  }

  case class Status(name: String, value: String)
  trait StatusProvider extends QueryParamProvider[Status]

  object keys {
    final val LOGIN_LOGOUT_KEY = DataKey.create[LoginLogoutProvider]("LOGIN_LOGOUT_KEY")

    final val LISTS_PROVIDER_KEY = DataKey.create[ListsQueryParamProvider]("LISTS_QUERYPARAM_PROVIDER_KEY")

    final val DIFFICULTIES_PROVIDER_KEY = DataKey.create[DifficultiesProvider]("DIFFICULTIES_PROVIDER_KEY")

    final val STATUS_PROVIDER_KEY = DataKey.create[StatusProvider]("STATUS_PROVIDER_KEY")
  }

  object groups {
    final val HACKERRANK_TOOLBAR_GROUP = "CodeEpiphany.Dojos.Hackerrank.Toolbar"
    final val TOOLBAR_PLACE            = "CodeEpiphany.Dojos"
    final val TITLE_TOOLBAR_GROUP      = "CodeEpiphany.Dojos.TitleToolbar"
    final val TITLE_TOOLBAR_PLACE      = "CodeEpiphany.Dojos.TitleToolbar.Place"
  }
}
