package com.wenjunhuang.codeepiphany.model

import cats.Show
import cats.syntax.all.*
import org.typelevel.ci.CIString

import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.SubmissionResult.*

enum SubmissionResult(val value: String) {
  case Processing       extends SubmissionResult("processing")
  case Success          extends SubmissionResult("success")
  case Failure          extends SubmissionResult("failure")
  case CompilationError extends SubmissionResult("compilationError")
  case Timeout          extends SubmissionResult("timeout")
  case RuntimeError     extends SubmissionResult("runtimeError")
  case Unknown          extends SubmissionResult("unknown")

  def showAsHtml: String =
    this match
      case Processing =>
        s"<html><font color='${SUBMISSION_PROCESSING_COLOR}'>${Processing.show}</font></html>"
      case Success =>
        s"<html><font color='${SUBMISSION_SUCCESS_COLOR}'>${Success.show}</font></html>"
      case Failure =>
        s"<html><font color='${SUBMISSION_FAILURE_COLOR}'>${Failure.show}</font></html>"
      case CompilationError =>
        s"<html><font color='${SUBMISSION_COMPILEERROR_COLOR}'>${CompilationError.show}</font></html>"
      case Timeout =>
        s"<html><font color='${SUBMISSION_TIMEOUT_COLOR}'>${Timeout.show}</font></html>"
      case RuntimeError =>
        s"<html><font color='${SUBMISSION_RUNTIMEERROR_COLOR}'>${Timeout.show}</font></html>"
      case Unknown =>
        s"<html><font color='${SUBMISSION_UNKNOWN_COLOR}'>${Unknown.show}</font></html>"
}

object SubmissionResult {
  implicit val showInst: Show[SubmissionResult] =
    Show.show[SubmissionResult] {
      case Processing       => PluginBundle.message("submissionResult.processing")
      case Success          => PluginBundle.message("submissionResult.success")
      case Failure          => PluginBundle.message("submissionResult.failure")
      case CompilationError => PluginBundle.message("submissionResult.compilationError")
      case RuntimeError     => PluginBundle.message("submissionResult.runtimeError")
      case Timeout          => PluginBundle.message("submissionResult.timeout")
      case Unknown          => PluginBundle.message("submissionResult.unknown")
    }

  def fromCIString(ciString: CIString): Option[SubmissionResult] = {
    if ciString == CIString(Success.value) then Some(Success)
    else if ciString == CIString(Failure.value) then Some(Failure)
    else if ciString == CIString(CompilationError.value) then Some(CompilationError)
    else if ciString == CIString(Timeout.value) then Some(Timeout)
    else if ciString == CIString(RuntimeError.value) then Some(Timeout)
    else if ciString == CIString(Unknown.value) then Some(Unknown)
    else if ciString == CIString(Processing.value) then Some(Processing)
    else None
  }

  val SUBMISSION_SUCCESS_COLOR      = "#1ab8a3"
  val SUBMISSION_FAILURE_COLOR      = "#ff375f"
  val SUBMISSION_PROCESSING_COLOR   = "#ffc01e"
  val SUBMISSION_COMPILEERROR_COLOR = "#ff4f64"
  val SUBMISSION_TIMEOUT_COLOR      = "#ff5164"
  val SUBMISSION_RUNTIMEERROR_COLOR      = "#ff5164"
  val SUBMISSION_UNKNOWN_COLOR      = "#ffc01e"
}
