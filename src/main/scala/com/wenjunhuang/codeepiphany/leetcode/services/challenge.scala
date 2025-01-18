package com.wenjunhuang.codeepiphany.leetcode.services

import cats.effect.{ Async, Concurrent }
import cats.effect.implicits.*
import cats.syntax.all.*
import java.io.File
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{ MessageDialogBuilder, Messages }
import com.intellij.openapi.util.text.StringUtil

import com.wenjunhuang.codeepiphany.database.Tables.{ CHALLENGE, CHALLENGE_LANGUAGE }
import com.wenjunhuang.codeepiphany.leetcode.model.*
import com.wenjunhuang.codeepiphany.leetcode.settings.{ LeetCodeCNSettings, LeetCodeCNSettingsConfigurable }
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.{ ChallengeId, ChallengeLanguageId }
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
              ShowSettingsUtil.getInstance().showSettingsDialog(project, classOf[LeetCodeCNSettingsConfigurable])
            None
          else Some((state.sourceFolder.get, language, state.fileNameTemplate.get, state.codeTemplate.get))
        case None =>
          val r = MessageDialogBuilder
            .yesNo("Error", "Please set the source folder and language in the settings")
            .ask(project)
          if r then ShowSettingsUtil.getInstance().showSettingsDialog(project, classOf[LeetCodeCNSettingsConfigurable])
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
    val api = LeetCodeApi[F](codeDojo)
    api
      .getQuestionData(challengeSlug)
      .map { content =>
        val pattern = """^([a-zA-Z]*)(\d*)$""".r
        content.codeSnippets.find { snippet =>
          snippet.langSlug match
            case pattern(lang, ver) =>
              Language.fromCIString(CIString(lang)).contains(language) && LanguageVersion.fromString(
                ver
              ) == languageVersion
        }.map { codeSnippet =>
          LeetCodeChallengeCodeTemplate(
            dojoId = content.frontendQuestionId,
            dojo = codeDojo,
            name = content.translatedTitle.filter(_.nonEmpty).getOrElse(content.title),
            code = codeSnippet.code,
            slug = content.titleSlug,
            description = content.translatedContent.filter(_.nonEmpty).getOrElse(content.content),
            difficulty = codeDojo.fromLeetCodeDifficulty(content.difficulty).value,
            language = language,
            languageVersion = languageVersion
          )
        }
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
        case Some(template) =>
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
                  storeChallengeToDatabase(project, codeDojo, template)
                ).parTupled.map { case (file, (challengeId, challengeLangId)) =>
                  val settings = ChallengeSettings.getInstance(project)
                  settings.addChallenge(
                    file.get.getCanonicalPath,
                    ChallengeSettingsStateItem(challengeId, challengeLangId, codeDojo, language, None)
                  )
                  file
                }
          }.sequence.flatMap {
            case Left(e)   => Async[F].delay(Messages.showErrorDialog(e.getMessage, "Error")).evalOnEDTAny()
            case Right(vf) => vf.fold(Async[F].unit)(openTextEditor(_, project).void)
          }
      }
      .handleErrorWith(e => Logger[F].warn(e)("Failed to open challenge"))
  }

  def storeChallengeToDatabase[F[_]: Async](
    project: Project,
    codeDojo: CodeDojo,
    challenge: LeetCodeChallengeCodeTemplate
  ): F[(ChallengeId, ChallengeLanguageId)] = {
    val repository = ChallengeRepository.getInstance(project)
    repository.getDSLContextResource.use { client =>
      Async[F].blocking {
        client.transactionResult { trx =>
          val dsl = trx.dsl()
          val challengeRecord = dsl.fetchOne(
            CHALLENGE,
            CHALLENGE.DOJO.eq(codeDojo.value).and(CHALLENGE.DOJOID.eq(challenge.dojoId))
          ) match {
            case null => dsl.newRecord(CHALLENGE).setId(IdGenerator.nextId())
            case r    => r
          }
          challengeRecord.setDescription(challenge.description)
          challengeRecord.setDifficulty(challenge.difficulty)
          challengeRecord.setDojo(challenge.dojo.value)
          challengeRecord.setDojoid(challenge.dojoId)
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

          (ChallengeId(challengeRecord.getId), ChallengeLanguageId(challengeLanguageRecord.getId))
        }
      }
    }
  }
}
