package com.wenjunhuang.codeepiphany.model

import cats.syntax.all.*
import cats.Show
import java.net.HttpCookie
import javax.swing.Icon
import org.typelevel.ci.CIString
import scala.annotation.static

enum CodeDojo(val domain: CIString, val value: String) {
  case HackerRank extends CodeDojo(CIString("hackerrank.com"), "hackerrank")
  case LeetCode   extends CodeDojo(CIString("leetcode.com"), "leetcode")
  case LeetCodeCN extends CodeDojo(CIString("leetcode.cn"), "leetcodecn")
  case CodeForces extends CodeDojo(CIString("codeforces.com"), "codeforces")

  def getIcon: Option[Icon] = this match {
    case HackerRank => Some(icons.CodeEpiphanyIcons.Dojos.HACKERRANK)
    case LeetCode   => Some(icons.CodeEpiphanyIcons.Dojos.LEETCODE)
    case LeetCodeCN => Some(icons.CodeEpiphanyIcons.Dojos.LEETCODE)
    case CodeForces => Some(icons.CodeEpiphanyIcons.Dojos.CODEFORCES)
  }

  def getLoginURL: String = this match {
    case HackerRank => "https://www.hackerrank.com/auth/login"
    case LeetCode   => "https://leetcode.com/accounts/login/"
    case LeetCodeCN => "https://leetcode.cn/accounts/login/"
    case CodeForces => "https://codeforces.com/enter"
  }

  def loginCandidateCookies(cookies: List[HttpCookie]): Boolean = this match {
    case HackerRank => cookies.exists(_.getName == "remember_hacker_token")
    case LeetCode   => cookies.exists(cookie => cookie.getName == "LEETCODE_SESSION" && cookie.getValue.nonEmpty)
    case LeetCodeCN => cookies.exists(cookie => cookie.getName == "LEETCODE_SESSION" && cookie.getValue.nonEmpty)
    case CodeForces => cookies.exists(cookie => cookie.getName == "X-User" && cookie.getValue.nonEmpty)
  }

  def requiresCodeRegionEnclosure: Boolean = {
    this match {
      case HackerRank => true
      case LeetCode   => true
      case LeetCodeCN => true
      case CodeForces => false
    }
  }

}

object CodeDojo {
  implicit val codeDojoShow: Show[CodeDojo] = Show.show {
    case HackerRank => "HackerRank"
    case LeetCode   => "LeetCode"
    case LeetCodeCN => "力扣"
    case CodeForces => "CodeForces"
  }

  @static
  def show(dojo: CodeDojo): String = dojo.show

  def fromCIHostname(s: CIString): Option[CodeDojo] = s match {
    case _ if s.contains(HackerRank.domain) => Some(CodeDojo.HackerRank)
    case _ if s.contains(LeetCode.domain)   => Some(CodeDojo.LeetCode)
    case _ if s.contains(LeetCodeCN.domain) => Some(CodeDojo.LeetCodeCN)
    case _ if s.contains(CodeForces.domain) => Some(CodeDojo.CodeForces)
    case _                                  => None
  }

  private val ALL_DOJOS = CodeDojo.values.map { dojo => CIString(dojo.value) -> dojo }.toMap

  def fromCIString(s: CIString): Option[CodeDojo] =
    ALL_DOJOS.get(s)
}
