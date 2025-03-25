package com.wenjunhuang.codeepiphany.leetcode.actions

import cats.Show
import cats.syntax.all.*
import org.typelevel.ci.CIString

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.leetcode.actions.InterviewPeriodParameterAction.{
  INTERVIEW_PERIOD_PROVIDER_KEY,
  InterviewPeriod,
  InterviewPeriodProvider
}
import com.wenjunhuang.codeepiphany.utils.actions.{ ParameterComboBoxAction, ParameterProvider }
import com.wenjunhuang.codeepiphany.PluginBundle
import io.circe.*

class InterviewPeriodParameterAction
    extends ParameterComboBoxAction[InterviewPeriod, InterviewPeriodProvider](
      INTERVIEW_PERIOD_PROVIDER_KEY,
      item => item.show,
      item => Option(item.show),
      item => None
    ) {}

object InterviewPeriodParameterAction {
  val INTERVIEW_PERIOD_PROVIDER_KEY: DataKey[InterviewPeriodProvider] =
    DataKey.create[InterviewPeriodProvider]("LEETCODE_INTERVIEW_PERIOD_PROVIDER_KEY")

  enum InterviewPeriod(val slug: String) {
    case ThirtyDays        extends InterviewPeriod("thirty-days")
    case ThreeMonths       extends InterviewPeriod("three-months")
    case SixMonths         extends InterviewPeriod("six-months")
    case MoreThanSixMonths extends InterviewPeriod("more-than-six-months")
    case All               extends InterviewPeriod("all")
  }

  object InterviewPeriod {
    implicit val show: Show[InterviewPeriod] = Show.show {
      case ThirtyDays        => PluginBundle.message("leetcode.interviewPeriod.30days")
      case ThreeMonths       => PluginBundle.message("leetcode.interviewPeriod.3Months")
      case SixMonths         => PluginBundle.message("leetcode.interviewPeriod.6Months")
      case MoreThanSixMonths => PluginBundle.message("leetcode.interviewPeriod.moreThan6Months")
      case All               => PluginBundle.message("leetcode.interviewPeriod.all")
    }

    def fromCIString(value: CIString): Option[InterviewPeriod] =
      if value == CIString(ThirtyDays.slug) then Some(ThirtyDays)
      else if value == CIString(ThreeMonths.slug) then Some(ThreeMonths)
      else if value == CIString(SixMonths.slug) then Some(SixMonths)
      else if value == CIString(MoreThanSixMonths.slug) then Some(MoreThanSixMonths)
      else if value == CIString(All.slug) then Some(All)
      else None

    implicit val circeEncoder: Encoder[InterviewPeriod] = Encoder.encodeString.contramap[InterviewPeriod](_.slug)
    implicit val circeDecoder: Decoder[InterviewPeriod] =
      Decoder.decodeString.emap(v => fromCIString(CIString(v)).toRight("Unknown interview period value"))
  }

  trait InterviewPeriodProvider extends ParameterProvider[InterviewPeriod] {}
}
