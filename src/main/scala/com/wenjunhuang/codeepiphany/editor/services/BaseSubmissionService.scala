package com.wenjunhuang.codeepiphany.editor.services

import cats.effect.kernel.Async
import cats.effect.Concurrent
import org.jooq.DSLContext
import org.typelevel.log4cats.LoggerFactory
import cats.syntax.all.*
import java.time.LocalDateTime
import org.jooq.impl.DSL
import scala.jdk.OptionConverters.*

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{ VirtualFile, VirtualFileUtil }

import com.wenjunhuang.codeepiphany.model.ChallengeRepository.SubmissionId
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, CodeDojo, Language, SubmissionResult }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.utils.IdGenerator
import fs2.Stream

abstract class BaseSubmissionService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](
  protected val myProject: Project,
  protected val myCodeDojo: CodeDojo
) {
  type SubmissionRequest
  type SubmissionResponse

  protected case class SubmissionResponseInfo(result: SubmissionResult, message: String, dojoSubmissionId: String)

  def submitCode(vf: VirtualFile): F[Unit] = {
    for {
      item          <- findSettingItem(vf)
      localCode     <- readLocalCode(vf)
      processedCode <- extractCode(localCode, item.language)
      request       <- prepareSubmissionRequest(item)
      submissionId  <- createSubmission(item, localCode, processedCode)
      _             <- executeSubmission(request, submissionId, processedCode)
    } yield ()
  }

  protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): F[SubmissionRequest]
  protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: SubmissionResponse
  ): SubmissionResponseInfo
  protected def callApi(basicInfo: SubmissionRequest, processedCode: String): Stream[F, SubmissionResponse]
  protected def reportSubmitResult(lastResponseInfo: SubmissionResponseInfo, lastResponse: SubmissionResponse): F[Unit]

  private def executeSubmission(
    basicInfo: SubmissionRequest,
    submissionId: SubmissionId,
    processedCode: String
  ): F[Unit] = {
    callApi(basicInfo, processedCode).evalMap { response =>
      updateSubmissionRecord(submissionId, response).map((_, response))
    }.compile.last.map {
      case Some((lastResponseInfo, lastResponse)) => reportSubmitResult(lastResponseInfo, lastResponse)
      case None                                   => Async[F].unit
    }
  }

  private def updateSubmissionRecord(
    submissionId: SubmissionId,
    response: SubmissionResponse
  ): F[SubmissionResponseInfo] = {
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource[F]
      .use { client =>
        Async[F].delay {
          client.transactionResult { trx =>
            val dsl            = DSL.using(trx)
            val submissionInfo = updateSpecificSubmissionRecord(dsl, submissionId, response)
            dsl
              .selectFrom(SOLUTION_SUBMISSION)
              .where(SOLUTION_SUBMISSION.ID.eq(submissionId.value))
              .fetchOptional()
              .toScala
              .map { record =>
                record.setResult(submissionInfo.result.value)
                record.setMessage(submissionInfo.message)
                record.setDojosubmissionid(submissionInfo.dojoSubmissionId)
                record.store()
                submissionInfo
              }
              .getOrElse(throw new IllegalAccessException(s"Cannot find submission record for $submissionId"))
          }
        }
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

  private def createSubmission(
    item: ChallengeSettingsStateItem,
    localCode: String,
    processedCode: String
  ): F[SubmissionId] = {
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource[F]
      .use { dsl =>
        Async[F].delay {
          dsl.transactionResult { trx =>
            val ctx = DSL.using(trx)
            val record = ctx
              .newRecord(SOLUTION_SUBMISSION)
              .setId(IdGenerator.nextId())
              .setChallengelanguageid(item.challengeLanguageId)
              .setLocalcode(localCode)
              .setSubmitcode(processedCode)
              .setSubmitdatetime(LocalDateTime.now())
              .setSolutionid(item.solutionId)
              .setResult(SubmissionResult.Processing.value)
            record.store()
            SubmissionId(record.getId)
          }
        }
      }
  }
}
