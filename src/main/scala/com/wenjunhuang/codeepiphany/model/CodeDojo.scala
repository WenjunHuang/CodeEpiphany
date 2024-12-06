package com.wenjunhuang.codeepiphany.model
import cats.Show
import org.typelevel.ci.CIString

enum CodeDojo(val host: CIString) {
  case HackerRank extends CodeDojo(CIString("hackerrank.com"))
  case LeetCode   extends CodeDojo(CIString("leetcode.com"))
  case LeetCodeCN extends CodeDojo(CIString("leetcode.cn"))
}

object CodeDojo {
  implicit val codeDojoShow: Show[CodeDojo] = Show.show(_.toString)
  def fromHostname(s: CIString): Option[CodeDojo] = s match {
    case _ if s.contains(HackerRank.host) => Some(CodeDojo.HackerRank)
    case _ if s.contains(LeetCode.host)   => Some(CodeDojo.LeetCode)
    case _ if s.contains(LeetCodeCN.host) => Some(CodeDojo.LeetCodeCN)
    case _                                => None
  }

  def optionValueOf(s: String): Option[CodeDojo] =
    try
      Some(CodeDojo.valueOf(s))
    catch {
      case _: Throwable => None
    }
}
