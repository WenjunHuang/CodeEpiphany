package com.wenjunhuang.codeepiphany.services

import cats.data.ReaderT
import cats.effect.Async
import cats.syntax.all.*
import java.io.File
import org.jooq.DSLContext
import scala.jdk.OptionConverters.*

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.database.tables.records.{ChallengeLanguageRecord, ChallengeRecord}
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.editor.services.database.getOrCreateDefaultSolution
import com.wenjunhuang.codeepiphany.model.{ChallengeRepository, CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.model.newtypes.*
import com.wenjunhuang.codeepiphany.services.file.{openTextEditor, refreshAndFindFileByIoFile, saveTextWithConflictResolution}
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings.LanguageSettingsState
import com.wenjunhuang.codeepiphany.settings.dojo.BaseSettingsConfigurable
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.template.VelocityUtils
import com.wenjunhuang.codeepiphany.utils.IdGenerator

abstract class BaseOpenChallengeService[F[_]: Async, Req, Template](
  protected val myProject: Project,
  protected val myCodeDojo: CodeDojo,
  protected val myConfigClass: Class[? <: BaseSettingsConfigurable]
) {

  case class ServiceState(
    sourceFolder: File,
    language: Language,
    languageVersion: LanguageVersion,
    fileNameTemplate: FileNameTemplate,
    codeTemplate: CodeTemplate,
    req: Req,
    template: Template,
    codeDojoChallengeId: CodeDojoChallengeId
  )

  protected def findLanguageSetting(language: Language, languageVersion: LanguageVersion): Option[LanguageSettingsState]
  protected def fillChallengeRecord(record: ChallengeRecord, state: ServiceState): Unit
  protected def storeExtraChallengeData(
    dsl: DSLContext,
    challenge: Template,
    challengeRecord: ChallengeRecord,
    challengeLanguageRecord: ChallengeLanguageRecord
  ): Unit

  protected def createTemplate(
    req: Req,
    language: Language,
    languageVersion: LanguageVersion
  ): F[(CodeDojoChallengeId, Template)]

  def openChallenge(req: Req, language: Language, languageVersion: LanguageVersion): F[Unit] = {
    for {
      settingsResult <- prepareState(req, language, languageVersion)
      _              <- processChallenge().run(settingsResult)
    } yield ()
  }

  private def processChallenge(): ReaderT[F, ServiceState, Unit] = {
    for {
      state            <- ReaderT.ask[F, ServiceState]
      (fileName, code) <- generateFileContent()
      codeFile         <- handleFileOperations(buildFileObject(state.sourceFolder, fileName, state.language), code)
      _                <- ReaderT.liftF(openTextEditor(codeFile, myProject))
    } yield ()
  }

  private def buildFileObject(sourceFolder: File, fileName: String, language: Language) = {
    new File(sourceFolder, s"${StringUtil.trim(fileName)}.${language.fileExt}")
  }

  private def handleFileOperations(file: File, code: String): ReaderT[F, ServiceState, VirtualFile] = {
    ChallengeSettings.getInstance(myProject).findChallengeId(file.getCanonicalPath) match {
      case Some(_) => ReaderT.liftF(refreshAndFindFileByIoFile(file))

      case None => createNewChallenge(file, code)
    }
  }
  private def generateFileContent(): ReaderT[F, ServiceState, (String, String)] = {
    for {
      state <- ReaderT.ask[F, ServiceState]
      result <- ReaderT.liftF(
        (
          VelocityUtils.generateContent(state.fileNameTemplate.value, state.template),
          VelocityUtils.generateContent(state.codeTemplate.value, state.template)
        ).mapN((_, _)).liftTo[F]
      )
    } yield result
  }

  private def createNewChallenge(file: File, code: String): ReaderT[F, ServiceState, VirtualFile] = {
    for {
      vf <- ReaderT.liftF(
        saveTextWithConflictResolution(file, code)
          .flatMap(_.liftTo[F](new Exception("Failed to save file")))
          .flatMap(refreshAndFindFileByIoFile)
      )
      (challengeId, langId, solutionId) <- storeChallengeToDatabase(code)
      _                                 <- updateChallengeSettings(vf, challengeId, langId, solutionId)
    } yield vf
  }

  private def updateChallengeSettings(
    vf: VirtualFile,
    challengeId: ChallengeId,
    challengeLanguageId: ChallengeLanguageId,
    solutionId: SolutionId
  ): ReaderT[F, ServiceState, VirtualFile] =
    for {
      state <- ReaderT.ask[F, ServiceState]
      vf <- ReaderT.liftF(Async[F].delay {
        ChallengeSettings
          .getInstance(myProject)
          .addChallenge(
            vf,
            ChallengeSettingsStateItem(challengeId, challengeLanguageId, myCodeDojo, state.language, solutionId)
          )
        vf
      })
    } yield vf

  private def storeChallengeToDatabase(
    code: String
  ): ReaderT[F, ServiceState, (ChallengeId, ChallengeLanguageId, SolutionId)] = {
    for {
      state <- ReaderT.ask[F, ServiceState]
      result <- ReaderT.liftF(ChallengeRepository.getInstance(myProject).getDSLContextResource.use { client =>
        Async[F].blocking {
          client.transactionResult { trx =>
            val dsl = trx.dsl()
            val challengeRecord = dsl.fetchOne(
              CHALLENGE,
              CHALLENGE.DOJO.eq(CodeDojo.HackerRank.value).and(CHALLENGE.DOJOID.eq(state.codeDojoChallengeId.value))
            ) match {
              case null => dsl.newRecord(CHALLENGE).setId(IdGenerator.nextId())
              case r    => r
            }
            fillChallengeRecord(challengeRecord, state)
            challengeRecord.store()

            val challengeLanguageRecord = dsl
              .fetchOptional(
                CHALLENGE_LANGUAGE,
                CHALLENGE_LANGUAGE.CHALLENGEID
                  .eq(challengeRecord.getId)
                  .and(CHALLENGE_LANGUAGE.LANGUAGE.eq(state.language.value))
                  .and(CHALLENGE_LANGUAGE.LANGUAGEVERSION.eq(state.languageVersion.version))
              )
              .toScala match
              case None =>
                dsl
                  .newRecord(CHALLENGE_LANGUAGE)
                  .setId(IdGenerator.nextId())
              case Some(r) => r
            challengeLanguageRecord.setChallengeid(challengeRecord.getId)
            challengeLanguageRecord.setLanguage(state.language.value)
            challengeLanguageRecord.setLanguageversion(state.languageVersion.version)
            challengeLanguageRecord.setCodetemplate(StringUtil.convertLineSeparators(code))
            challengeLanguageRecord.store()

            storeExtraChallengeData(dsl, state.template, challengeRecord, challengeLanguageRecord)

            val defaultSolutionId = getOrCreateDefaultSolution(dsl, challengeRecord.getId)

            (
              ChallengeId(challengeRecord.getId),
              ChallengeLanguageId(challengeLanguageRecord.getId),
              SolutionId(defaultSolutionId)
            )
          }
        }
      })
    } yield result
  }

  private def prepareState(req: Req, language: Language, languageVersion: LanguageVersion): F[ServiceState] = {
    for {
      setting <- Async[F].delay {
        findLanguageSetting(language, languageVersion) match
          case Some(state) if isSettingsValid(state, language, languageVersion) =>
            state
          case _ =>
            handleInvalidSettings()
            throw new Exception("Settings are invalid")
      }.evalOnEDTAny()
      (codeDojoChallengeId, template) <- createTemplate(req, language, languageVersion)
    } yield {
      ServiceState(
        File(setting.sourceFolder.get),
        language,
        languageVersion,
        FileNameTemplate(setting.fileNameTemplate.get),
        CodeTemplate(setting.codeTemplate.get),
        req,
        template,
        codeDojoChallengeId
      )
    }
  }

  private def handleInvalidSettings(): Unit = {
    val result = MessageDialogBuilder
      .yesNo("Error", "Please configure source folder and language settings")
      .ask(myProject)
    if (result) {
      ShowSettingsUtil.getInstance().showSettingsDialog(myProject, myConfigClass)
    }
  }

  private def isSettingsValid(
    state: LanguageSettingsState,
    language: Language,
    languageVersion: LanguageVersion
  ): Boolean = {
    state.sourceFolder.exists(it => File(it).isDirectory) &&
    state.language.contains(language) &&
    state.languageVersion.contains(languageVersion) &&
    state.fileNameTemplate.nonEmpty &&
    state.codeTemplate.nonEmpty
  }
}
