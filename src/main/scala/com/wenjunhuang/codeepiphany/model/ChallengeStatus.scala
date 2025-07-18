package com.wenjunhuang.codeepiphany.model

import cats.Show
import com.wenjunhuang.codeepiphany.PluginBundle
import io.circe.*
import org.typelevel.ci.CIString

enum ChallengeStatus(val value: String) {
  case Solved   extends ChallengeStatus("solved")
  case Unsolved extends ChallengeStatus("unsolved")
  case Tried    extends ChallengeStatus("tried")
}

object ChallengeStatus {
  implicit val showInstance: Show[ChallengeStatus] = Show.show[ChallengeStatus] {
    case Solved   => PluginBundle.message("challenge.status.solved")
    case Unsolved => PluginBundle.message("challenge.status.unsolved")
    case Tried    => PluginBundle.message("challenge.status.tried")
  }

  def fromCIString(value: CIString): Option[ChallengeStatus] =
    if value == CIString(Solved.value) then Some(Solved)
    else if value == CIString(Unsolved.value) then Some(Unsolved)
    else if value == CIString(Tried.value) then Some(Tried)
    else None

  implicit val circeEncoder: Encoder[ChallengeStatus] = Encoder.encodeString.contramap[ChallengeStatus](_.value)
  implicit val circeDecoder: Decoder[ChallengeStatus] =
    Decoder.decodeString.emap(v => fromCIString(CIString(v)).toRight("Unknown status value"))
}
