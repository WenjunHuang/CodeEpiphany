package com.wenjunhuang.codeepiphany.model

import cats.syntax.all.*
import cats.Show
import java.net.HttpCookie
import javax.swing.Icon
import org.typelevel.ci.CIString
import scala.annotation.static

import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.atcoder.models.AtCoderDifficulty

enum CodeDojo(val domain: CIString, val value: String) {
  case HackerRank extends CodeDojo(CIString("hackerrank.com"), "hackerrank")
  case LeetCode   extends CodeDojo(CIString("leetcode.com"), "leetcode")
  case LeetCodeCN extends CodeDojo(CIString("leetcode.cn"), "leetcodecn")
  case CodeForces extends CodeDojo(CIString("codeforces.com"), "codeforces")
  case AtCoder    extends CodeDojo(CIString("atcoder.jp"), "atcoder")

  def getIcon: Option[Icon] = this match {
    case HackerRank => Some(icons.CodeEpiphanyIcons.Dojos.HACKERRANK)
    case LeetCode   => Some(icons.CodeEpiphanyIcons.Dojos.LEETCODE)
    case LeetCodeCN => Some(icons.CodeEpiphanyIcons.Dojos.LEETCODE)
    case CodeForces => Some(icons.CodeEpiphanyIcons.Dojos.CODEFORCES)
    case AtCoder    => Some(icons.CodeEpiphanyIcons.Dojos.ATCODER)
  }

  def getLoginURL: String = this match {
    case HackerRank => "https://www.hackerrank.com/auth/login"
    case LeetCode   => "https://leetcode.com/accounts/login/"
    case LeetCodeCN => "https://leetcode.cn/accounts/login/"
    case CodeForces => "https://codeforces.com/enter"
    case AtCoder    => "https://atcoder.jp/login"
  }

  def loginCandidateCookies(cookies: List[HttpCookie]): Boolean = this match {
    case HackerRank => cookies.exists(_.getName == "remember_hacker_token")
    case LeetCode   => cookies.exists(cookie => cookie.getName == "LEETCODE_SESSION" && cookie.getValue.nonEmpty)
    case LeetCodeCN => cookies.exists(cookie => cookie.getName == "LEETCODE_SESSION" && cookie.getValue.nonEmpty)
    case CodeForces => cookies.exists(cookie => cookie.getName == "X-User" && cookie.getValue.nonEmpty)
    case AtCoder =>
      cookies.exists(cookie =>
        cookie.getName == "REVEL_SESSION" && cookie.getValue.nonEmpty && cookie.getValue.contains("SessionKey")
      )
  }

  def requiresCodeRegionEnclosure: Boolean = {
    this match {
      case HackerRank => true
      case LeetCode   => true
      case LeetCodeCN => true
      case CodeForces => true
      case AtCoder    => true
    }
  }

  def difficultyShow(difficulty: String): String = {
    this match {
      case HackerRank => difficulty
      case LeetCode   => difficulty
      case LeetCodeCN => difficulty
      case CodeForces => difficulty
      case AtCoder    => difficulty
    }
  }

  def difficultyShowAsHtml(difficulty: String): String = {
    this match {
      case AtCoder =>
        if StringUtil.isEmpty(difficulty) then ""
        else
          difficulty.toIntOption match {
            case Some(difficulty) => AtCoderDifficulty.fromInt(difficulty).showAsHtml
            case _                => ""
          }
      case _ => ""
    }
  }
}

object CodeDojo {
  implicit val codeDojoShow: Show[CodeDojo] = Show.show {
    case HackerRank => "HackerRank"
    case LeetCode   => "LeetCode"
    case LeetCodeCN => "LeetCodeCN"
    case CodeForces => "CodeForces"
    case AtCoder    => "AtCoder"
  }

  @static
  def show(dojo: CodeDojo): String = dojo.show

  def fromCIHostname(s: CIString): Option[CodeDojo] = s match {
    case _ if s.contains(HackerRank.domain) => Some(CodeDojo.HackerRank)
    case _ if s.contains(LeetCode.domain)   => Some(CodeDojo.LeetCode)
    case _ if s.contains(LeetCodeCN.domain) => Some(CodeDojo.LeetCodeCN)
    case _ if s.contains(CodeForces.domain) => Some(CodeDojo.CodeForces)
    case _ if s.contains(AtCoder.domain)    => Some(CodeDojo.AtCoder)
    case _                                  => None
  }

  private val ALL_DOJOS = CodeDojo.values.map { dojo => CIString(dojo.value) -> dojo }.toMap

  def fromCIString(s: CIString): Option[CodeDojo] =
    ALL_DOJOS.get(s)
}
