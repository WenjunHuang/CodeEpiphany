package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.{ Async, Concurrent }
import cats.syntax.all.*
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{ MessageDialogBuilder, Messages }
import com.wenjunhuang.codeepiphany.hackerrank.model.{ ChallengeCodeTemplate, Contest }
import com.wenjunhuang.codeepiphany.hackerrank.settings.{ HackerRankSettings, HackerRankSettingsConfigurable }
import com.wenjunhuang.codeepiphany.model.{ Language, LanguageVersion }
import com.wenjunhuang.codeepiphany.services.editor.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.template.VelocityUtils
import org.typelevel.log4cats.Logger

import java.io.File

object editor {
  def openChallenge[F[_]: Async: Concurrent: HttpClientKeeper: Logger](
    project: Project,
    challengeSlug: String,
    contest: Contest
  ): F[Unit] = {
    Async[F].delay {
      val settings = HackerRankSettings.getInstance(project)
      val state    = settings.getState
      if state.sourceFolder.isEmpty || state.language.isEmpty then
        val r = MessageDialogBuilder
          .yesNo("Error", "Please set the source folder and language in the settings")
          .ask(project)
        if r then ShowSettingsUtil.getInstance().showSettingsDialog(project, classOf[HackerRankSettingsConfigurable])

        None
      else Some((state.sourceFolder.get, state.language.get, state.fileNameTemplate.get, state.codeTemplate.get))
    }.evalOnEDTAny().flatMap {
      case None => Async[F].unit
      case Some((sourceFolder, language, fileNameTemplate, codeTemplate)) =>
        fetchChallengeContentAndOpen(
          project,
          challengeSlug,
          contest,
          language,
          sourceFolder,
          fileNameTemplate,
          codeTemplate
        )
    }
  }

  private def fetchChallengeContentAndOpen[F[_]: Async: Concurrent: HttpClientKeeper: Logger](
    project: Project,
    challengeSlug: String,
    contest: Contest,
    language: Language,
    sourceFolder: String,
    fileNameTemplate: String,
    codeTemplate: String
  ): F[Unit] = {
    val api = HackerRankApi[F]()
    api
      .getChallengeContent(challengeSlug, contest)
      .map {
        case Some(content) =>
          content.codeTemplates.filter { case ((lang, _), _) =>
            lang == language
          }.toList.maxByOption { case ((_, version), _) => version }.map { case ((_, _), temp) =>
            ChallengeCodeTemplate(
              content.detail.id,
              content.detail.name,
              content.detail.slug,
              content.detail.preview.getOrElse(""),
              temp.header,
              temp.template,
              temp.tail
            )
          }
        case None => None
      }
      .flatMap {
        case None => Async[F].delay(Messages.showInfoMessage("Failed to open challenge", "Error")).evalOnEDTAny()
        case Some(template) =>
          (
            VelocityUtils.generateContent(fileNameTemplate, template),
            VelocityUtils.generateContent(codeTemplate, template)
          ).mapN { (fileName, code) =>
            val file = new File(File(sourceFolder), s"$fileName.${language.fileExt}")
            saveTextToFileAndRefresh(file, code)
              .flatMap(vf => openTextEditor(vf, project))
              .void
          }.sequence.flatMap {
            case Left(e)  => Async[F].delay(Messages.showErrorDialog(e.getMessage, "Error")).evalOnEDTAny()
            case Right(_) => Async[F].unit
          }
      }
      .handleErrorWith(e => Logger[F].warn(e)("Failed to open challenge"))
  }

}
