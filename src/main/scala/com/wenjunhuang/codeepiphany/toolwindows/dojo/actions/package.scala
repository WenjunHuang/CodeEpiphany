package com.wenjunhuang.codeepiphany.toolwindows.dojo

import com.intellij.openapi.actionSystem.DataKey
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.toolwindows.dojo.actions.providers.*

package object actions {
  trait LoginLogoutProvider {
    def login(): Unit
    def logout(): Unit
    def isLoggingIn: Boolean
    def hasLoggedIn: Boolean
  }

  case class Category(name: String, value: String, marker: Any = null)
  trait CategoryProvider extends QueryParamProvider[Category] {}

  case class DifficultyData(name: String, value: String)
  trait DifficultiesProvider extends QueryParamProvider[DifficultyData] {}

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

  enum PageSize(val value: Int) {
    case Twenty     extends PageSize(20)
    case Fifty      extends PageSize(50)
    case OneHundred extends PageSize(100)

    def show: String =
      this match
        case Twenty     => PluginBundle.message("hackerrank.ui.query.pagesize.20")
        case Fifty      => PluginBundle.message("hackerrank.ui.query.pagesize.50")
        case OneHundred => PluginBundle.message("hackerrank.ui.query.pagesize.100")
  }

  object PageSize {
    def fromInt(value: Int): Option[PageSize] =
      value match
        case Twenty.value     => Some(Twenty)
        case Fifty.value      => Some(Fifty)
        case OneHundred.value => Some(OneHundred)
        case _                => None
  }
  trait PaginationProvider extends QueryParamProvider[PageSize] {
    def getPageSize: Int
    def getCurrentPage: Int
    def setCurrentPage(page: Int): Unit
    def getTotalPages: Int
    def getTotalItems: Int
    def refresh(): Unit
  }

  object keys {
    final val LOGIN_LOGOUT_KEY = DataKey.create[LoginLogoutProvider]("LOGIN_LOGOUT_KEY")

    final val LISTS_PROVIDER_KEY = DataKey.create[CategoryProvider]("LISTS_QUERYPARAM_PROVIDER_KEY")

    final val DIFFICULTIES_PROVIDER_KEY = DataKey.create[DifficultiesProvider]("DIFFICULTIES_PROVIDER_KEY")

    final val STATUS_PROVIDER_KEY = DataKey.create[StatusProvider]("STATUS_PROVIDER_KEY")

    final val SKILL_PROVIDER_KEY = DataKey.create[SkillProvider]("SKILL_PROVIDER_KEY")

    final val TAG_PROVIDER_KEY = DataKey.create[TagProvider]("TAG_PROVIDER_KEY")

    final val PAGINATION_PROVIDER_KEY = DataKey.create[PaginationProvider]("PAGINATION_PROVIDER_KEY")

    final val SWITCHUI_PROVIDER_KEY = DataKey.create[SwitchUIProvider]("SWITCHUI_PROVIDER_KEY")

    final val CHALLENGE_PROVIDER_KEY = DataKey.create[ChallengeProvider]("CHALLENGE_PROVIDER_KEY")
  }
}
