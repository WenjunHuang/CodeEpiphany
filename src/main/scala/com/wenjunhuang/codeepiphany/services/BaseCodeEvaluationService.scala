package com.wenjunhuang.codeepiphany.services

import cats.effect.IO
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{VfsUtilCore, VirtualFile}
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, SubmissionResult}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.{ChallengeSettingsStateItem, TestCase}
import fs2.Stream

abstract class BaseCodeEvaluationService(protected val myProject: Project, protected val myCodeDojo: CodeDojo) {
  type EvaluationRequest
  type EvaluationResponse
  protected case class EvaluationResponseInfo(result: SubmissionResult, message: String)

  def evaluateCode(vf: VirtualFile, customTestCases: Option[List[TestCase]]): IO[Unit] = {
    for {
      item          <- findSettingItem(vf)
      localCode     <- readLocalCode(vf)
      processedCode <- extractCode(localCode, item.language)
      request       <- prepareRequest(item, customTestCases)
      _             <- executeEvaluation(request, processedCode, customTestCases)
    } yield ()
  }

  protected def prepareRequest(
    item: ChallengeSettingsStateItem,
    customTestCases: Option[List[TestCase]]
  ): IO[EvaluationRequest]

  protected def callApi(
    request: EvaluationRequest,
    code: String,
    customTestCases: Option[List[TestCase]]
  ): Stream[IO, EvaluationResponse]

  protected def handleEvaluationResponse(
    response: EvaluationResponse,
    request: EvaluationRequest,
    code: String,
    customTestCases: Option[List[TestCase]]
  ): IO[EvaluationResponseInfo]
  protected def reportEvaluationResult(
    lastResponseInfo: EvaluationResponseInfo,
    lastResponse: EvaluationResponse
  ): IO[Unit]

  private def executeEvaluation(
    request: EvaluationRequest,
    code: String,
    customTestCases: Option[List[TestCase]]
  ): IO[Unit] = {
    callApi(request, code, customTestCases).evalMap { response =>
      handleEvaluationResponse(response, request, code, customTestCases).map((_, response))
    }.compile.last.flatMap {
      case Some((lastResponseInfo, lastResponse)) => reportEvaluationResult(lastResponseInfo, lastResponse)
      case None                                   => IO.unit
    }
  }

  private def findSettingItem(vf: VirtualFile): IO[ChallengeSettingsStateItem] =
    IO.delay {
      val settings = ChallengeSettings.getInstance(myProject)
      settings.findChallengeId(vf) match
        case Some(item) if item.dojo == myCodeDojo => item
        case _ => throw new IllegalAccessException(s"Cannot find setting for ${vf.getName}")
    }

  private def readLocalCode(vf: VirtualFile): IO[String] =
    IO.blocking {
      VfsUtilCore.loadText(vf)
    }

  private def extractCode(rawCode: String, language: Language): IO[String] =
    IO.delay {
      if myCodeDojo.requiresCodeRegionEnclosure then language.extractCodeFromRegion(rawCode)
      else rawCode
    }
}
