package com.wenjunhuang.codeepiphany.codeforces.services

import cats.effect.{Async, Concurrent}
import cats.effect.implicits.*
import cats.syntax.all.*
import java.io.File
import org.typelevel.log4cats.Logger

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{MessageDialogBuilder, Messages}
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.codeforces.models.{CodeForcesChallengeCodeTemplate, CodeForcesChallengeData}
import com.wenjunhuang.codeepiphany.codeforces.settings.{CodeForcesSettings, CodeForcesSettingsConfigurable}
import com.wenjunhuang.codeepiphany.database.tables.records.CodeforcesProblemsetsRecord
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.{ChallengeId, ChallengeLanguageId, SolutionId}
import com.wenjunhuang.codeepiphany.services.file.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.template.VelocityUtils

object challenge {
  def openChallenge[F[_]: Async: Concurrent: HttpClientManager: Logger](
    project: Project,
    record: CodeforcesProblemsetsRecord,
    language: Language,
    languageVersion: LanguageVersion
  ): F[Unit] = {
    Async[F].delay {
      val settings = CodeForcesSettings.getInstance(project)
      settings.getLanguageSetting(language, languageVersion) match
        case Some(state) =>
          if state.sourceFolder.isEmpty || state.language.isEmpty then
            val r = MessageDialogBuilder
              .yesNo("Error", "Please set the source folder and language in the settings")
              .ask(project)
            if r then
              ShowSettingsUtil.getInstance().showSettingsDialog(project, classOf[CodeForcesSettingsConfigurable])
            None
          else Some((state.sourceFolder.get, language, state.fileNameTemplate.get, state.codeTemplate.get))
        case None =>
          val r = MessageDialogBuilder
            .yesNo("Error", "Please set the source folder and language in the settings")
            .ask(project)
          if r then ShowSettingsUtil.getInstance().showSettingsDialog(project, classOf[CodeForcesSettingsConfigurable])
          None
    }.evalOnEDTAny().flatMap {
      case None => Async[F].unit
      case Some((sourceFolder, language, fileNameTemplate, codeTemplate)) =>
        fetchChallengeContentAndOpen(
          project,
          record,
          language,
          languageVersion,
          sourceFolder,
          fileNameTemplate,
          codeTemplate
        )
    }
  }

  private def fetchChallengeContentAndOpen[F[_]: Async: Concurrent: HttpClientManager: Logger](
    project: Project,
    record: CodeforcesProblemsetsRecord,
    language: Language,
    languageVersion: LanguageVersion,
    sourceFolder: String,
    fileNameTemplate: String,
    codeTemplate: String
  ): F[Unit] = {
    val api = CodeForcesApi[F]()
    api
      .getChallengeData(Option(record.getProblemsetname), record.getContestid, record.getIndex)
      .flatMap {
        case None =>
          Async[F]
            .delay(Messages.showInfoMessage(s"Challenge ${record.getName} is not available", "Information"))
            .evalOnEDTAny()
        case Some(challengeData) =>
          val template = CodeForcesChallengeCodeTemplate(
            contestIdIndex = s"${Option(record.getContestid).map(_.toString).getOrElse("")}${record.getIndex}",
            name = record.getName,
            language = language,
            languageVersion = languageVersion,
            rating = Option(record.getRating).map(_.toInt),
            problemsetName = Option(record.getProblemsetname),
            tags = record.getTags.split(",").toList
          )
          (
            VelocityUtils.generateContent(fileNameTemplate, template),
            VelocityUtils.generateContent(codeTemplate, template)
          ).mapN { (fileName, code) =>
            val trimmedFileName = StringUtil.trim(fileName)
            val file            = new File(File(sourceFolder), s"$trimmedFileName.${language.fileExt}")
            val settings        = ChallengeSettings.getInstance(project)
            settings.findChallengeId(file.getCanonicalPath) match
              case Some(_) =>
                refreshAndFindFileByIoFile(file)
              case None =>
                (
                  saveTextToFile(file, code).flatMap(refreshAndFindFileByIoFile),
                  storeChallengeToDatabase(project,
                    record,
                    challengeData)
                ).parTupled.map { case (file, (challengeId, challengeLangId, solutionId)) =>
                  val settings = ChallengeSettings.getInstance(project)
                  settings.addChallenge(
                    file,
                    ChallengeSettingsStateItem(challengeId, challengeLangId, CodeDojo.CodeForces, language, solutionId)
                  )
                  file
                }
          }.sequence.flatMap {
            case Left(e)   => Async[F].delay(Messages.showErrorDialog(e.getMessage, "Error")).evalOnEDTAny()
            case Right(vf) => openTextEditor(vf, project).void
          }
      }
      .handleErrorWith(e => Logger[F].warn(e)("Failed to open challenge"))
  }

  def storeChallengeToDatabase[F[_]: Async](
    project: Project,
    record: CodeforcesProblemsetsRecord,
    challengeData: CodeForcesChallengeData
  ): F[(ChallengeId, ChallengeLanguageId, SolutionId)] = ???
}
