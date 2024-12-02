package com.wenjunhuang.codeepiphany.model

enum CodeDojo {
  case HackerRank
  case LeetCode
  case LeetCodeCN
}

object CodeDojo {
  def fromHostname(s: String): Option[CodeDojo] = s match {
    case _ if s.contains("hackerrank.com") => Some(CodeDojo.HackerRank)
    case _ if s.contains("leetcode.com")   => Some(CodeDojo.LeetCode)
    case _ if s.contains("leetcode.cn")    => Some(CodeDojo.LeetCodeCN)
    case _                                 => None
  }

  def optionValueOf(s: String): Option[CodeDojo] = try
    Some(CodeDojo.valueOf(s))
  catch {
    case _: Throwable => None
  }
}
