package com.wenjunhuang.codeepiphany.luogu.services

import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.effect.syntax.all.*
import cats.syntax.all.*
import fs2.Stream
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import org.jooq.{DSLContext, Record}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory
import scala.jdk.OptionConverters.*
import scodec.bits.ByteVector

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{MessageDialogBuilder, Messages, MessageUtil}
import com.intellij.util.ui.ImageUtil

import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.database.Tables.{CHALLENGE, CHALLENGE_LANGUAGE}
import com.wenjunhuang.codeepiphany.luogu.models.LuoGuSubmissionResponse
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettingsConfigurable
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion, SubmissionResult}
import com.wenjunhuang.codeepiphany.model.newtypes.SubmissionId
import com.wenjunhuang.codeepiphany.model.CodeDojo.LuoGu
import com.wenjunhuang.codeepiphany.services.{console, BaseSubmissionService, ChallengeRepository}
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem

class LuoGuSubmissionService[F[_]: Async: Concurrent: HttpClientManager: LoggerFactory](project: Project)
    extends BaseSubmissionService[F](project, LuoGu) {
  override type SubmissionRequest  = Request
  override type SubmissionResponse = LuoGuSubmissionResponse

  override protected def prepareSubmissionRequest(item: ChallengeSettingsStateItem): F[Request] =
    ChallengeRepository
      .getInstance(myProject)
      .getDSLContextResource[F]
      .use { client => Async[F].delay(createRequest(item, client)) }

  override protected def updateSpecificSubmissionRecord(
    dsl: DSLContext,
    submissionId: SubmissionId,
    response: SubmissionResponse
  ): SubmissionResponseInfo = SubmissionResponseInfo(response.result, response.message, response.submissionId)

  override protected def callApi(basicInfo: Request, processedCode: String): Stream[F, SubmissionResponse] = {
    LuoGuApi[F]()
      .submitAnswer(basicInfo.problemId, basicInfo.languageId, processedCode, showCaptcha)
  }

  override protected def reportSubmitResult(
    lastResponseInfo: SubmissionResponseInfo,
    lastResponse: SubmissionResponse
  ): F[Unit] = {
    lastResponseInfo.result match
      case SubmissionResult.Success =>
        console.info[F](project, s"🎉 Passed!")
      case _ =>
        console.error[F](project, s"${lastResponseInfo.result.show}\n${lastResponseInfo.message}")
  }

  private def createRequest(item: ChallengeSettingsStateItem, client: DSLContext): Request = {
    client
      .select(CHALLENGE.DOJOID, CHALLENGE_LANGUAGE.LANGUAGE, CHALLENGE_LANGUAGE.LANGUAGEVERSION)
      .from(CHALLENGE)
      .innerJoin(CHALLENGE_LANGUAGE)
      .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
      .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
      .fetchOptional()
      .toScala
      .flatMap(parseRecord)
      .getOrElse(throw new IllegalStateException("Cannot find challenge data"))
  }

  private def parseRecord(record: Record): Option[Request] = {
    for {
      problemId <- Option(record.get(CHALLENGE.DOJOID))
      language  <- Language.fromCIString(CIString(record.get(CHALLENGE_LANGUAGE.LANGUAGE)))
      langVer = LanguageVersion.fromString(record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION))
      programTypeId <- resolveLanguageId(language, langVer)
    } yield Request(problemId, language, langVer, programTypeId)
  }
  private def resolveLanguageId(language: Language, version: LanguageVersion): Option[String] =
    LuoGuSettingsConfigurable.LUOGU_LANGUAGES.get((language, version))

  private def showCaptcha(captcha: ByteVector): F[String] = {
    Async[F].delay {
      val captchaImage = ImageIO.read(captcha.toInputStream)
      MessageDialogBuilder
        .okCancel("Captcha", "Please solve the captcha")
        .title("Captcha")
        .icon(ImageUtil.toIcon(captchaImage))
        .message("Please solve the captcha")
        .input("Captcha", "")
        .show()
        .map(_.orNull)
    }.evalOnEDTDefault()
  }

  case class Request(problemId: String, language: Language, langVer: LanguageVersion, languageId: String)
}
