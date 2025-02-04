package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.{Async, Concurrent}
import cats.syntax.all.*
import java.io.File
import org.jooq.DSLContext
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{MessageDialogBuilder, Messages}
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.database.tables.records.{ChallengeLanguageRecord, ChallengeRecord}
import com.wenjunhuang.codeepiphany.editor.services.database.getOrCreateDefaultSolution
import com.wenjunhuang.codeepiphany.hackerrank.model.{HackerRankChallengeCodeTemplate, HackerRankChallengeContent, HackerRankContest, HackerRankLanguageTemplate}
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.newtypes.*
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.file.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.settings.dojo.BaseCodeDojoSettings.LanguageSettingsState
import com.wenjunhuang.codeepiphany.settings.dojo.BaseSettingsConfigurable
import com.wenjunhuang.codeepiphany.utils.IdGenerator
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.template.VelocityUtils

class HackerRankOpenChallengeService[F[_]: Async: Concurrent: HttpClientManager: Logger](
  private val myProject: Project,
  private val myConfigClass: Class[? <: BaseSettingsConfigurable]
) {

  case class SettingValidationResult(
    sourceFolder: String,
    language: Language,
    languageVersion: LanguageVersion,
    fileNameTemplate: String,
    codeTemplate: String
  )

  def openChallenge(
    challengeSlug: String,
    contest: HackerRankContest,
    language: Language,
    languageVersion: LanguageVersion
  ): F[Unit] = {
    for {
      settingsResult <- validateSettings(language, languageVersion)
      _ <- settingsResult match
        case None => Async[F].unit
        case Some(validationResult) =>
          processChallenge(
            challengeSlug,
            contest,
            validationResult.language,
            validationResult.languageVersion,
            validationResult.sourceFolder,
            validationResult.fileNameTemplate,
            validationResult.codeTemplate
          )
    } yield ()
  }

  private def validateSettings(
    language: Language,
    languageVersion: LanguageVersion
  ): F[Option[SettingValidationResult]] = {
    Async[F].delay {
      val settings = HackerRankSettings.getInstance(myProject)
      settings.getLanguageSetting(language, languageVersion) match
        case Some(state) if isSettingsValid(state) =>
          SettingValidationResult(
            state.sourceFolder.get,
            language,
            languageVersion,
            state.fileNameTemplate.get,
            state.codeTemplate.get
          ).some
        case _ =>
          handleInvalidSettings()
          None
    }.evalOnEDTAny()
  }

  private def isSettingsValid(state: LanguageSettingsState): Boolean = {
    state.sourceFolder.nonEmpty &&
    state.language.nonEmpty &&
    state.languageVersion.nonEmpty &&
    state.fileNameTemplate.nonEmpty &&
    state.codeTemplate.nonEmpty
  }

  // 统一处理无效配置
  private def handleInvalidSettings(): Unit = {
    val result = MessageDialogBuilder
      .yesNo("Error", "Please configure source folder and language settings")
      .ask(myProject)
    if (result) {
      ShowSettingsUtil.getInstance().showSettingsDialog(myProject, myConfigClass)
    }
  }

  private def processChallenge(
    challengeSlug: String,
    contest: HackerRankContest,
    language: Language,
    languageVersion: LanguageVersion,
    sourceFolder: String,
    fileNameTemplate: String,
    codeTemplate: String
  ): F[Unit] =
    HackerRankApi[F]()
      .getChallengeContent(challengeSlug, contest)
      .flatMap { content =>
        content.codeTemplates.get((language, languageVersion)) match
          case Some(temp) => Async[F].pure(handleValidTemplate(content, temp, contest, language, languageVersion))
          case None => showUnsupportedLanguageMessage(challengeSlug, language, languageVersion) >> Async[F].pure(None)
      }
      .flatMap {
        case Some(template) =>
          createAndOpenChallengeFile(template, sourceFolder, fileNameTemplate, codeTemplate)
        case None => Async[F].unit
      }
      .handleErrorWith(e => Logger[F].warn(e)("Failed to open challenge"))

  private def showUnsupportedLanguageMessage(
    challengeSlug: String,
    language: Language,
    languageVersion: LanguageVersion
  ): F[Unit] = {
    Async[F]
      .delay(
        Messages.showInfoMessage(
          s"Challenge '${challengeSlug}' does not support ${language.show}${languageVersion.version}",
          "Information"
        )
      )
      .evalOnEDTAny()
  }

  // 模板处理逻辑
  private def handleValidTemplate(
    content: HackerRankChallengeContent,
    temp: HackerRankLanguageTemplate,
    contest: HackerRankContest,
    language: Language,
    languageVersion: LanguageVersion
  ) = {
    val template = HackerRankChallengeCodeTemplate(
      content.detail.id.toString,
      HackerRank,
      content.detail.name,
      content.detail.slug,
      content.detail.bodyHtml.getOrElse(""),
      temp.header,
      temp.template,
      temp.tail,
      contest.slug,
      ChallengeDifficulty.fromCIString(CIString(content.detail.difficultyName)).get.value,
      language,
      languageVersion
    )
    Some(template)
  }

  // 文件创建与打开逻辑
  private def createAndOpenChallengeFile(
    template: HackerRankChallengeCodeTemplate,
    sourceFolder: String,
    fileNameTemplate: String,
    codeTemplate: String
  ): F[Unit] = {
    for {
      (fileName, code) <- generateFileContent(fileNameTemplate, codeTemplate, template)
      codeFile <- handleFileOperations(buildFileObject(sourceFolder, fileName, template.language), code, template)
      _        <- openTextEditor(codeFile, myProject)
    } yield ()
  }

  // 内容生成逻辑
  private def generateFileContent(
    fileNameTemplate: String,
    codeTemplate: String,
    template: HackerRankChallengeCodeTemplate
  ): F[(String, String)] = {
    (VelocityUtils.generateContent(fileNameTemplate, template), VelocityUtils.generateContent(codeTemplate, template))
      .mapN((_, _))
      .liftTo[F]
  }

  // 文件路径构建
  private def buildFileObject(sourceFolder: String, fileName: String, language: Language) = {
    val trimmedName = StringUtil.trim(fileName)
    new File(sourceFolder, s"$trimmedName.${language.fileExt}")
  }

  // 文件存储与数据库操作
  private def handleFileOperations(
    file: File,
    code: String,
    template: HackerRankChallengeCodeTemplate
  ): F[VirtualFile] = {
    ChallengeSettings.getInstance(myProject).findChallengeId(file.getCanonicalPath) match {
      case Some(_) => refreshAndFindFileByIoFile(file)

      case None => createNewChallenge(file, code, template)
    }
  }

  // 新挑战创建流程
  private def createNewChallenge(
    file: File,
    code: String,
    template: HackerRankChallengeCodeTemplate
  ): F[VirtualFile] = {
    for {
      vf <- saveTextWithConflictResolution(file, code)
        .flatMap(_.liftTo[F](new Exception("Failed to save file")))
        .flatMap(refreshAndFindFileByIoFile)
      (challengeId, langId, solutionId) <- storeChallengeToDatabase(template)
      _ <- updateChallengeSettings(vf, challengeId, langId, template.language, solutionId)
    } yield vf
  }

  private def updateChallengeSettings(
    vf: VirtualFile,
    challengeId: ChallengeId,
    challengeLanguageId: ChallengeLanguageId,
    language: Language,
    solutionId: SolutionId
  ): F[VirtualFile] = Async[F].blocking {
    ChallengeSettings
      .getInstance(myProject)
      .addChallenge(vf, ChallengeSettingsStateItem(challengeId, challengeLanguageId, HackerRank, language, solutionId))
    vf
  }

  private def storeChallengeToDatabase(
    challenge: HackerRankChallengeCodeTemplate
  ): F[(ChallengeId, ChallengeLanguageId, SolutionId)] = {
    val repository = ChallengeRepository.getInstance(myProject)
    repository.getDSLContextResource.use { client =>
      Async[F].blocking {
        client.transactionResult { trx =>
          val dsl = trx.dsl()
          val challengeRecord = dsl.fetchOne(
            CHALLENGE,
            CHALLENGE.DOJO.eq(CodeDojo.HackerRank.value).and(CHALLENGE.DOJOID.eq(challenge.id))
          ) match {
            case null => dsl.newRecord(CHALLENGE).setId(IdGenerator.nextId())
            case r    => r
          }
          challengeRecord.setDescription(challenge.description)
          challengeRecord.setDifficulty(challenge.difficulty)
          challengeRecord.setDojo(CodeDojo.HackerRank.value)
          challengeRecord.setDojoid(challenge.id)
          challengeRecord.setSlug(challenge.slug)
          challengeRecord.setTitle(challenge.name)
          challengeRecord.store()

          val challengeLanguageRecord = dsl.fetchOne(
            CHALLENGE_LANGUAGE,
            CHALLENGE_LANGUAGE.CHALLENGEID
              .eq(challengeRecord.getId)
              .and(CHALLENGE_LANGUAGE.LANGUAGE.eq(challenge.language.value))
              .and(CHALLENGE_LANGUAGE.LANGUAGEVERSION.eq(challenge.languageVersion.version))
          ) match
            case null =>
              dsl
                .newRecord(CHALLENGE_LANGUAGE)
                .setId(IdGenerator.nextId())
            case r => r
          challengeLanguageRecord.setChallengeid(challengeRecord.getId)
          challengeLanguageRecord.setLanguage(challenge.language.value)
          challengeLanguageRecord.setLanguageversion(challenge.languageVersion.version)
          challengeLanguageRecord.setCodetemplate(StringUtil.convertLineSeparators(challenge.getCode))
          challengeLanguageRecord.store()

          storeExtraChallengeData(challenge, dsl, challengeRecord, challengeLanguageRecord)

          val defaultSolutionId = getOrCreateDefaultSolution(dsl, challengeRecord.getId)

          (
            ChallengeId(challengeRecord.getId),
            ChallengeLanguageId(challengeLanguageRecord.getId),
            SolutionId(defaultSolutionId)
          )
        }
      }
    }
  }

  private def storeExtraChallengeData(
    challenge: HackerRankChallengeCodeTemplate,
    dsl: DSLContext,
    challengeRecord: ChallengeRecord,
    challengeLanguageRecord: ChallengeLanguageRecord
  ) = {
    val hackerRankRecord =
      (dsl.fetchOne(HACKERRANK_CHALLENGE, HACKERRANK_CHALLENGE.ID.eq(challengeRecord.getId)) match
        case null =>
          dsl.newRecord(HACKERRANK_CHALLENGE).setId(challengeRecord.getId)
        case r => r
      ).setContest(challenge.contest)
        .setContestslug(challenge.contest)
    hackerRankRecord.store()

    val hackerRankLangRecord = dsl.fetchOne(
      HACKERRANK_CHALLENGE_LANGUAGE,
      HACKERRANK_CHALLENGE_LANGUAGE.ID.eq(challengeLanguageRecord.getId)
    ) match
      case null => dsl.newRecord(HACKERRANK_CHALLENGE_LANGUAGE).setId(challengeLanguageRecord.getId)
      case r    => r
    hackerRankLangRecord.setCodeheader(StringUtil.convertLineSeparators(challenge.header))
    hackerRankLangRecord.setCodetemplate(StringUtil.convertLineSeparators(challenge.template))
    hackerRankLangRecord.setCodetail(StringUtil.convertLineSeparators(challenge.tail))
    hackerRankLangRecord.store()
  }
}
