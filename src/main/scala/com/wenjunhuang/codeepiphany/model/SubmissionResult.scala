package com.wenjunhuang.codeepiphany.model

import cats.Show
import cats.syntax.all.*
import com.wenjunhuang.codeepiphany.PluginBundle
import com.wenjunhuang.codeepiphany.model.SubmissionResult.*
import org.typelevel.ci.CIString

enum SubmissionResult(val value: String) {
  case Processing          extends SubmissionResult("processing")
  case Success             extends SubmissionResult("success")
  case Failure             extends SubmissionResult("failure")
  case CompilationError    extends SubmissionResult("compilationError")
  case Timeout             extends SubmissionResult("timeout")
  case MemoryLimitExceeded extends SubmissionResult("memoryLimit")
  case OutputLimitExceeded extends SubmissionResult("outputLimit")
  case RuntimeError        extends SubmissionResult("runtimeError")
  case InternalError       extends SubmissionResult("internalError")
  case Unknown             extends SubmissionResult("unknown")

  def showAsHtml: String =
    this match
      case Processing =>
        s"<html><font color='${SubmissionStatusColors.Processing}'>${Processing.show}</font></html>"
      case Success =>
        s"<html><font color='${SubmissionStatusColors.Success}'>${Success.show}</font></html>"
      case Failure =>
        s"<html><font color='${SubmissionStatusColors.Failure}'>${Failure.show}</font></html>"
      case CompilationError =>
        s"<html><font color='${SubmissionStatusColors.CompilationError}'>${CompilationError.show}</font></html>"
      case Timeout =>
        s"<html><font color='${SubmissionStatusColors.Timeout}'>${Timeout.show}</font></html>"
      case MemoryLimitExceeded =>
        s"<html><font color='${SubmissionStatusColors.MemoryLimitExceeded}'>${MemoryLimitExceeded.show}</font></html>"
      case RuntimeError =>
        s"<html><font color='${SubmissionStatusColors.RuntimeError}'>${RuntimeError.show}</font></html>"
      case OutputLimitExceeded =>
        s"<html><font color='${SubmissionStatusColors.OutputLimitExceeded}'>${OutputLimitExceeded.show}</font></html>"
      case InternalError =>
        s"<html><font color='${SubmissionStatusColors.InternalError}'>${InternalError.show}</font></html>"
      case Unknown =>
        s"<html><font color='${SubmissionStatusColors.Unknown}'>${Unknown.show}</font></html>"
}

object SubmissionResult {
  implicit val showInst: Show[SubmissionResult] =
    Show.show[SubmissionResult] {
      case Processing          => PluginBundle.message("submissionResult.processing")
      case Success             => PluginBundle.message("submissionResult.success")
      case Failure             => PluginBundle.message("submissionResult.failure")
      case CompilationError    => PluginBundle.message("submissionResult.compilationError")
      case RuntimeError        => PluginBundle.message("submissionResult.runtimeError")
      case Timeout             => PluginBundle.message("submissionResult.timeout")
      case MemoryLimitExceeded => PluginBundle.message("submissionResult.memoryLimit")
      case OutputLimitExceeded => PluginBundle.message("submissionResult.outputLimit")
      case InternalError       => PluginBundle.message("submissionResult.internalError")
      case Unknown             => PluginBundle.message("submissionResult.unknown")
    }

  def fromCIString(ciString: CIString): Option[SubmissionResult] = {
    if ciString == CIString(Success.value) then Some(Success)
    else if ciString == CIString(Failure.value) then Some(Failure)
    else if ciString == CIString(CompilationError.value) then Some(CompilationError)
    else if ciString == CIString(Timeout.value) then Some(Timeout)
    else if ciString == CIString(RuntimeError.value) then Some(RuntimeError)
    else if ciString == CIString(Unknown.value) then Some(Unknown)
    else if ciString == CIString(Processing.value) then Some(Processing)
    else None
  }
}

object SubmissionStatusColors {

  // 处理中（动态状态）
  val Processing = "#2196F3" // 蓝色 - 类似Azure Blue
  // 成功状态
  val Success = "#00C853" // Material Green 600
  // 失败类错误
  val Failure = "#FF5252" // Material Red A200
  // 编译错误
  val CompilationError = "#FF9800" // Material Orange 500
  // 资源限制类
  val Timeout             = "#9C27B0" // Material Purple 500
  val MemoryLimitExceeded = "#E040FB" // Material Purple A200
  val OutputLimitExceeded = "#D500F9" // Material Purple A400
  // 运行时错误
  val RuntimeError = "#D32F2F" // Material Red 700
  // 系统级错误
  val InternalError = "#B71C1C" // Material Red 900
  // 未知状态
  val Unknown = "#9E9E9E" // Material Grey 500
}
