package com.wenjunhuang.codeepiphany.model

import cats.Show
import cats.syntax.all.*
import com.intellij.openapi.util.text.StringUtil
import com.wenjunhuang.codeepiphany.utils.Colors
import com.wenjunhuang.codeepiphany.PluginBundle
import org.typelevel.ci.CIString

enum SubmissionResult(val value: String) {
  case Success          extends SubmissionResult("success")
  case Failure          extends SubmissionResult("failure")
  case CompilationError extends SubmissionResult("compilation_error")
  case Timeout          extends SubmissionResult("timeout")
  case Unknown          extends SubmissionResult("unknown")

  def showAsHtml: String =
    this match
      case Success =>
        s"<html><font color='${Colors.DIFFICULTY_EASY_COLOR}'>${Success.show}</font></html>"
      case Failure =>
        s"<html><font color='${Colors.DIFFICULTY_MEDIUM_COLOR}'>${Failure.show}</font></html>"
      case CompilationError =>
        s"<html><font color='${Colors.DIFFICULTY_HARD_COLOR}'>${CompilationError.show}</font></html>"
      case Timeout =>
        s"<html><font color='${Colors.DIFFICULTY_ADVANCED_COLOR}'>${Timeout.show}</font></html>"
      case Unknown =>
        s"<html><font color='${Colors.DIFFICULTY_EXPERT_COLOR}'>${Unknown.show}</font></html>"
}

object SubmissionResult {
  implicit val showInst: Show[SubmissionResult] =
    Show.show[SubmissionResult](it => PluginBundle.message(s"submissionResult.${StringUtil.decapitalize(it.toString)}"))

  def fromCIString(ciString: CIString): Option[SubmissionResult] = {
    if ciString == CIString(Success.value) then Some(Success)
    else if ciString == CIString(Failure.value) then Some(Failure)
    else if ciString == CIString(CompilationError.value) then Some(CompilationError)
    else if ciString == CIString(Timeout.value) then Some(Timeout)
    else if ciString == CIString(Unknown.value) then Some(Unknown)
    else None
  }
}
