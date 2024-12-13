package com.wenjunhuang.codeepiphany.controllers.dojo

import com.intellij.openapi.actionSystem.DataKey

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
  trait ListsQueryParamProvider extends QueryParamProvider[ListQueryItem] {}

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

  case class Skill(name: String, value: String)
  trait SkillProvider extends QueryParamProvider[Skill]

  case class TagGroup(name: String, value: String, tags: List[Tag])
  case class Tag(name: String, value: String, groupValue: String)

  sealed trait TagProvider extends QueryParamProvider[Tag] {}

  trait SingleTagGroupProvider extends TagProvider {}
  trait MultiTagGroupProvider extends TagProvider {
    def isSearchEnabled: Boolean
    def searchTags(query: String): List[Tag]
  }

  object keys {
    final val LOGIN_LOGOUT_KEY = DataKey.create[LoginLogoutProvider]("LOGIN_LOGOUT_KEY")

    final val LISTS_PROVIDER_KEY = DataKey.create[ListsQueryParamProvider]("LISTS_QUERYPARAM_PROVIDER_KEY")

    final val DIFFICULTIES_PROVIDER_KEY = DataKey.create[DifficultiesProvider]("DIFFICULTIES_PROVIDER_KEY")

    final val STATUS_PROVIDER_KEY = DataKey.create[StatusProvider]("STATUS_PROVIDER_KEY")

    final val SKILL_PROVIDER_KEY = DataKey.create[SkillProvider]("SKILL_PROVIDER_KEY")

    final val TAG_PROVIDER_KEY = DataKey.create[TagProvider]("TAG_PROVIDER_KEY")
  }

  object groups {
    final val HACKERRANK_TOOLBAR_GROUP = "CodeEpiphany.Dojos.Hackerrank.Toolbar"
    final val TOOLBAR_PLACE            = "CodeEpiphany.Dojos"
    final val TITLE_TOOLBAR_GROUP      = "CodeEpiphany.Dojos.TitleToolbar"
    final val TITLE_TOOLBAR_PLACE      = "CodeEpiphany.Dojos.TitleToolbar.Place"
  }
}
