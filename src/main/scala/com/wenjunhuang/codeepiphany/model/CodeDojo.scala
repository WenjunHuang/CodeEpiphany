package com.wenjunhuang.codeepiphany.model
import cats.Show
import org.typelevel.ci.CIString

enum CodeDojo(val domain: CIString,val value:String) {
  case HackerRank extends CodeDojo(CIString("hackerrank.com"),"hackerrank")
  case LeetCode   extends CodeDojo(CIString("leetcode.com"),"leetcode")
  case LeetCodeCN extends CodeDojo(CIString("leetcode.cn"),"leetcodecn")
}

object CodeDojo {
  implicit val codeDojoShow: Show[CodeDojo] = Show.show(_.toString)
  
  def fromHostname(s: CIString): Option[CodeDojo] = s match {
    case _ if s.contains(HackerRank.domain) => Some(CodeDojo.HackerRank)
    case _ if s.contains(LeetCode.domain)   => Some(CodeDojo.LeetCode)
    case _ if s.contains(LeetCodeCN.domain) => Some(CodeDojo.LeetCodeCN)
    case _                                => None
  }

  def optionValueOf(s: String): Option[CodeDojo] =
    try
      Some(CodeDojo.valueOf(s))
    catch {
      case _: Throwable => None
    }
}
