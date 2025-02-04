package com.wenjunhuang.codeepiphany.services

import cats.effect.{ Async, Concurrent }
import cats.effect.kernel.Async
import cats.syntax.all.*
import fs2.Stream
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{ VirtualFile, VirtualFileUtil }

import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, SubmissionResult }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem

abstract class BaseCodeEvaluationService[F[_]: Async](
  protected val myProject: Project,
  protected val myCodeDojo: CodeDojo
) {
  type EvaluationRequest
  type EvaluationResponse
  protected case class EvaluationResponseInfo(result: SubmissionResult, message: String)

  def evaluateCode(vf: VirtualFile, customTestCases: Option[String]): F[Unit] = {
    for {
      item          <- findSettingItem(vf)
      localCode     <- readLocalCode(vf)
      processedCode <- extractCode(localCode, item.language)
      request       <- prepareRequest(item, customTestCases)
      _             <- executeEvaluation(request, processedCode, customTestCases)
    } yield ()
  }

  protected def prepareRequest(item: ChallengeSettingsStateItem, customTestCases: Option[String]): F[EvaluationRequest]

  protected def callApi(
    request: EvaluationRequest,
    code: String,
    customTestCases: Option[String]
  ): Stream[F, EvaluationResponse]

  protected def handleEvaluationResponse(
    response: EvaluationResponse,
    request: EvaluationRequest,
    code: String,
    customTestCases: Option[String]
  ): F[EvaluationResponseInfo]
  protected def reportEvaluationResult(
    lastResponseInfo: EvaluationResponseInfo,
    lastResponse: EvaluationResponse
  ): F[Unit]

  private def executeEvaluation(request: EvaluationRequest, code: String, customTestCases: Option[String]): F[Unit] = {
    callApi(request, code, customTestCases).evalMap { response =>
      handleEvaluationResponse(response, request, code, customTestCases).map((_, response))
    }.compile.last.flatMap {
      case Some((lastResponseInfo, lastResponse)) => reportEvaluationResult(lastResponseInfo, lastResponse)
      case None                                   => Async[F].unit
    }
  }

  private def findSettingItem(vf: VirtualFile): F[ChallengeSettingsStateItem] =
    Async[F].delay {
      val settings = ChallengeSettings.getInstance(myProject)
      settings.findChallengeId(vf) match
        case Some(item) if item.dojo == myCodeDojo => item
        case _ => throw new IllegalAccessException(s"Cannot find setting for ${vf.getName}")
    }

  private def readLocalCode(vf: VirtualFile): F[String] =
    Async[F].blocking {
      VirtualFileUtil.readText(vf)
    }

  private def extractCode(rawCode: String, language: Language): F[String] =
    Async[F].delay {
      if myCodeDojo.requiresCodeRegionEnclosure then language.extractCodeFromRegion(rawCode)
      else rawCode
    }
}
