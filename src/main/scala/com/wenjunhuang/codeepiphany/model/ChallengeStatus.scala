package com.wenjunhuang.codeepiphany.model

import cats.Show
import com.wenjunhuang.codeepiphany.PluginBundle

enum ChallengeStatus(val value: String) {
  case Solved   extends ChallengeStatus("solved")
  case Unsolved extends ChallengeStatus("unsolved")
}

object ChallengeStatus {
  implicit val showInstance: Show[ChallengeStatus] = Show.show[ChallengeStatus] {
    case Solved   => PluginBundle.message("challenge.status.solved")
    case Unsolved => PluginBundle.message("challenge.status.unsolved")
  }
}
