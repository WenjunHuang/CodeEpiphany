package com.wenjunhuang.codeepiphany.hackerrank.model

import org.typelevel.ci.CIString

enum HackerRankContest(val slug: String) {
  case Master       extends HackerRankContest("master")
  case ProjectEuler extends HackerRankContest("projecteuler")
}

object HackerRankContest {
  def fromCIString(cis: CIString): Option[HackerRankContest] =
    if cis == CIString(Master.slug) then Some(Master)
    else if cis == CIString(ProjectEuler.slug) then Some(ProjectEuler)
    else None
}
