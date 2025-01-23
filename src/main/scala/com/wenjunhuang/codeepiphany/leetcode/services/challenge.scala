package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.{ Async, Concurrent }
import cats.effect.implicits.*
import cats.syntax.all.*
import java.io.File
import org.typelevel.log4cats.Logger

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{ MessageDialogBuilder, Messages }
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.database.Tables.{ CHALLENGE, CHALLENGE_LANGUAGE, LEETCODE_CHALLENGE }
import com.wenjunhuang.codeepiphany.editor.services.database.getOrCreateDefaultSolution
import com.wenjunhuang.codeepiphany.leetcode.model.*
import com.wenjunhuang.codeepiphany.leetcode.settings.{ LeetCodeCNSettings, LeetCodeSettingsConfigurable }
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.{ ChallengeId, ChallengeLanguageId, SolutionId }
import com.wenjunhuang.codeepiphany.services.file.{ openTextEditor, refreshAndFindFileByIoFile, saveTextToFile }
import com.wenjunhuang.codeepiphany.services.http.HttpClientManager
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.IdGenerator
import com.wenjunhuang.codeepiphany.utils.template.VelocityUtils

object challenge {
  def openChallenge[F[_]: Async: Concurrent: HttpClientManager: Logger](
    project: Project,
    codeDojo: CodeDojo,
    challengeSlug: String,
    language: Language,
    languageVersion: LanguageVersion
  ): F[Unit] = {
    Async[F].delay {
      val settings = LeetCodeCNSettings.getInstance(project)
      settings.getLanguageSetting(language, languageVersion) match
        case Some(state) =>
          if state.sourceFolder.isEmpty || state.language.isEmpty then
            val r = MessageDialogBuilder
              .yesNo("Error", "Please set the source folder and language in the settings")
              .ask(project)
            if r then
              ShowSettingsUtil.getInstance().showSettingsDialog(project, classOf[LeetCodeSettingsConfigurable])
            None
          else Some((state.sourceFolder.get, language, state.fileNameTemplate.get, state.codeTemplate.get))
        case None =>
          val r = MessageDialogBuilder
            .yesNo("Error", "Please set the source folder and language in the settings")
            .ask(project)
          if r then ShowSettingsUtil.getInstance().showSettingsDialog(project, classOf[LeetCodeSettingsConfigurable])
          None
    }.evalOnEDTAny().flatMap {
      case None => Async[F].unit
      case Some((sourceFolder, language, fileNameTemplate, codeTemplate)) =>
        fetchChallengeContentAndOpen(
          project,
          codeDojo,
          challengeSlug,
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
    codeDojo: CodeDojo,
    challengeSlug: String,
    language: Language,
    languageVersion: LanguageVersion,
    sourceFolder: String,
    fileNameTemplate: String,
    codeTemplate: String
  ): F[Unit] = {
    LeetCodeApi[F](codeDojo)
      .getQuestionData(challengeSlug)
      .map { content =>
        content.codeSnippets.find { snippet =>
          codeDojo
            .fromLeetCodeLanguage(snippet.langSlug)
            .contains(language, languageVersion)
        }.map { codeSnippet => (content, codeSnippet) }
      }
      .flatMap {
        case None =>
          Async[F]
            .delay(
              Messages.showInfoMessage(
                s"Challenge '${challengeSlug}' does not support ${language.show}${languageVersion.version}",
                "Information"
              )
            )
            .evalOnEDTAny()
        case Some((content, codeSnippet)) =>
          val template = LeetCodeChallengeCodeTemplate(
            questionId = content.questionId,
            frontendQuestionId = content.frontendQuestionId,
            dojo = codeDojo,
            name = content.translatedTitle.filter(_.nonEmpty).getOrElse(content.title),
            code = codeSnippet.code,
            slug = content.titleSlug,
            description = content.translatedContent.filter(_.nonEmpty).getOrElse(content.content),
            difficulty = codeDojo.fromLeetCodeDifficulty(content.difficulty).value,
            language = language,
            languageVersion = languageVersion
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
                  storeChallengeToDatabase(project, codeDojo, template, content)
                ).parTupled.map { case (file, (challengeId, challengeLangId, solutionId)) =>
                  val settings = ChallengeSettings.getInstance(project)
                  settings.addChallenge(
                    file,
                    ChallengeSettingsStateItem(challengeId, challengeLangId, codeDojo, language, solutionId)
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
    codeDojo: CodeDojo,
    challenge: LeetCodeChallengeCodeTemplate,
    content: LeetCodeChallengeData
  ): F[(ChallengeId, ChallengeLanguageId, SolutionId)] = {
    val repository = ChallengeRepository.getInstance(project)
    repository.getDSLContextResource.use { client =>
      Async[F].blocking {
        client.transactionResult { trx =>
          val dsl = trx.dsl()
          val challengeRecord = dsl.fetchOne(
            CHALLENGE,
            CHALLENGE.DOJO.eq(codeDojo.value).and(CHALLENGE.DOJOID.eq(challenge.questionId))
          ) match {
            case null => dsl.newRecord(CHALLENGE).setId(IdGenerator.nextId())
            case r    => r
          }
          challengeRecord.setDescription(challenge.description)
          challengeRecord.setDifficulty(challenge.difficulty)
          challengeRecord.setDojo(challenge.dojo.value)
          challengeRecord.setDojoid(challenge.questionId)
          challengeRecord.setSlug(challenge.slug)
          challengeRecord.setTitle(challenge.name)
          challengeRecord.store()

          val leetCodeChallengeRecord = dsl
            .fetchOne(LEETCODE_CHALLENGE, LEETCODE_CHALLENGE.ID.eq(challengeRecord.getId)) match
            case null =>
              dsl.newRecord(LEETCODE_CHALLENGE).setId(challengeRecord.getId)
            case r => r

          leetCodeChallengeRecord
            .setFrontendquestionid(content.frontendQuestionId)
            .setTestcase(content.exampleTestcases)
          leetCodeChallengeRecord.store()

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
}
