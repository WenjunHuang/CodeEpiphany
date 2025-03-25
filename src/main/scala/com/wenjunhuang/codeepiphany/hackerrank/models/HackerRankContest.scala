package com.wenjunhuang.codeepiphany.hackerrank.models

import io.circe.{Decoder, Encoder}
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

  implicit val circeEncoder: Encoder[HackerRankContest] = Encoder.encodeString.contramap[HackerRankContest](_.slug)
  implicit val circeDecoder: Decoder[HackerRankContest] = Decoder.decodeString.emap {
    case Master.slug       => Right(Master)
    case ProjectEuler.slug => Right(ProjectEuler)
    case _                 => Left("Invalid HackerRank contest value")
  }
}
