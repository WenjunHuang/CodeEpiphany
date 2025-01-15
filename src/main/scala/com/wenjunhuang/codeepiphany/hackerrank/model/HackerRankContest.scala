package com.wenjunhuang.codeepiphany.hackerrank.model

import org.typelevel.ci.CIString

enum Contest(val slug: String) {
  case Master       extends Contest("master")
  case ProjectEuler extends Contest("projecteuler")
}

object Contest {
  def fromCIString(cis: CIString): Option[Contest] =
    if cis == CIString(Master.slug) then Some(Master)
    else if cis == CIString(ProjectEuler.slug) then Some(ProjectEuler)
    else None
}
