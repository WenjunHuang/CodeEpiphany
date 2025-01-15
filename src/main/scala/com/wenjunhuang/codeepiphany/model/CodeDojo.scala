package com.wenjunhuang.codeepiphany.model
import cats.Show
import org.typelevel.ci.CIString

import java.net.HttpCookie
import javax.swing.Icon

enum CodeDojo(val domain: CIString, val value: String) {
  case HackerRank extends CodeDojo(CIString("hackerrank.com"), "hackerrank")
  case LeetCode   extends CodeDojo(CIString("leetcode.com"), "leetcode")
  case LeetCodeCN extends CodeDojo(CIString("leetcode.cn"), "leetcodecn")

  def getIcon: Option[Icon] = this match {
    case HackerRank => Some(icons.CodeEpiphanyIcons.Dojos.HACKERRANK)
    case LeetCode   => Some(icons.CodeEpiphanyIcons.Dojos.LEETCODE)
    case LeetCodeCN => Some(icons.CodeEpiphanyIcons.Dojos.LEETCODE)
  }

  def getLoginURL: String = this match {
    case HackerRank => "https://www.hackerrank.com/auth/login"
    case LeetCode   => "https://leetcode.com/accounts/login/"
    case LeetCodeCN => "https://leetcode.cn/accounts/login/"
  }

  def loginCandidateCookies(cookies: List[HttpCookie]): Boolean = this match {
    case HackerRank => cookies.exists(_.getName == "remember_hacker_token")
    case LeetCode   => cookies.exists(_.getName == "leetcode.com")
    case LeetCodeCN => cookies.exists(_.getName == "leetcode.cn")
  }

}

object CodeDojo {
  implicit val codeDojoShow: Show[CodeDojo] = Show.show(_.toString)

  def fromCIHostname(s: CIString): Option[CodeDojo] = s match {
    case _ if s.contains(HackerRank.domain) => Some(CodeDojo.HackerRank)
    case _ if s.contains(LeetCode.domain)   => Some(CodeDojo.LeetCode)
    case _ if s.contains(LeetCodeCN.domain) => Some(CodeDojo.LeetCodeCN)
    case _                                  => None
  }

  private val ALL_DOJOS = CodeDojo.values.map { dojo => CIString(dojo.value) -> dojo }.toMap

  def fromCIString(s: CIString): Option[CodeDojo] =
    ALL_DOJOS.get(s)
}
