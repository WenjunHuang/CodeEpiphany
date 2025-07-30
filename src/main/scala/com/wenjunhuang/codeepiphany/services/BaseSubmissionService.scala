package com.wenjunhuang.codeepiphany.services

import cats.effect.IO
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{ VfsUtilCore, VirtualFile }
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.model.{ CodeDojo, Language, SubmissionResult }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.IdGenerator
import fs2.Stream
import org.jooq.DSLContext
import org.jooq.impl.DSL

import java.time.LocalDateTime
import scala.jdk.OptionConverters.*

abstract class BaseSubmissionService(protected val myProject: Project, protected val myCodeDojo: CodeDojo) {
  type SubmissionRequest
  type SubmissionResponse

  protected case class SubmissionResponseInfo(result: SubmissionResult, message: String, dojoSubmissionId: String)

  def submitCode(vf: VirtualFile): IO[Unit] = {
    for {
      item          <- findSettingItem(vf)
      localCode     <- readLocalCode(vf)
      processedCode <- extractCode(localCode, item.language)
      request       <- prepareSubmissionRequest(item)
      submissionId  <- createSubmission(item, localCode, processedCode)
      _ <- executeSubmission(request, submissionId, processedCode)
        .handleErrorWith(e => handleSubmissionError(e, request, submissionId, localCode, processedCode))
    } yield ()
  }

  protected def handleSubmissionError(
    error: Throwable,
    req: SubmissionRequest,
    submissionId: SubmissionId,
    localCode: String,
    processedCode: String
  ): IO[Unit] = {
    ChallengeRepository.getInstance(myProject).getDSLContextResource.use { dsl =>
      IO.delay {
        dsl
          .update(SOLUTION_SUBMISSION)
          .set(SOLUTION_SUBMISSION.RESULT, SubmissionResult.Unknown.value)
          .set(SOLUTION_SUBMISSION.MESSAGE, error.getMessage)
          .where(SOLUTION_SUBMISSION.ID.eq(submissionId.value))
          .execute()
      }
    } *> IO.raiseError(error)
  }

  protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): IO[SubmissionRequest]
  protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: SubmissionResponse
  ): SubmissionResponseInfo
  protected def callApi(basicInfo: SubmissionRequest, processedCode: String): Stream[IO, SubmissionResponse]
  protected def reportSubmitResult(
    basicInfo: SubmissionRequest,
    submissionId: SubmissionId,
    processedCode: String,
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: SubmissionResponse
  ): IO[Unit]

  private def executeSubmission(
    basicInfo: SubmissionRequest,
    submissionId: SubmissionId,
    processedCode: String
  ): IO[Unit] = {
    callApi(basicInfo, processedCode).evalMap { response =>
      updateSubmissionRecord(submissionId, response).map((_, response))
    }.compile.last.flatMap {
      case Some((lastResponseInfo, lastResponse)) =>
        reportSubmitResult(basicInfo, submissionId, processedCode, lastResponseInfo, lastResponse)
      case None => IO.unit
    }
  }

  private def updateSubmissionRecord(
    submissionId: SubmissionId,
    response: SubmissionResponse
  ): IO[SubmissionResponseInfo] = {
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource
      .use { client =>
        IO.delay {
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

  private def createSubmission(
    item: ChallengeSettingsStateItem,
    localCode: String,
    processedCode: String
  ): IO[SubmissionId] = {
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource
      .use { dsl =>
        IO.delay {
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
