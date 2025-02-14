package com.wenjunhuang.codeepiphany.leetcode.actions

import cats.Show
import cats.syntax.all.*

import com.intellij.openapi.actionSystem.DataKey

import com.wenjunhuang.codeepiphany.leetcode.actions.InterviewPeriodParameterAction.{INTERVIEW_PERIOD_PROVIDER_KEY, InterviewPeriod, InterviewPeriodProvider}
import com.wenjunhuang.codeepiphany.utils.actions.{ParameterComboBoxAction, ParameterProvider}
import com.wenjunhuang.codeepiphany.PluginBundle

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

  enum InterviewPeriod(val slug:String) {
    case ThirtyDays extends InterviewPeriod("thirty-days")
    case ThreeMonths extends InterviewPeriod("three-months")
    case SixMonths extends InterviewPeriod("six-months")
    case MoreThanSixMonths extends InterviewPeriod("more-than-six-months")
    case All extends InterviewPeriod("all")
  }

  object InterviewPeriod {
    implicit val show: Show[InterviewPeriod] = Show.show {
      case ThirtyDays        => PluginBundle.message("leetcode.interviewPeriod.30days")
      case ThreeMonths       => PluginBundle.message("leetcode.interviewPeriod.3Months")
      case SixMonths         => PluginBundle.message("leetcode.interviewPeriod.6Months")
      case MoreThanSixMonths => PluginBundle.message("leetcode.interviewPeriod.moreThan6Months")
      case All               => PluginBundle.message("leetcode.interviewPeriod.all")
    }
  }

  trait InterviewPeriodProvider extends ParameterProvider[InterviewPeriod] {}
}
