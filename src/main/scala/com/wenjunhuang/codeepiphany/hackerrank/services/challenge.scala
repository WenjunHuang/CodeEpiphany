package com.wenjunhuang.codeepiphany.hackerrank.services

import cats.effect.{ Async, Concurrent }
import cats.effect.implicits.*
import cats.syntax.all.*
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.{ MessageDialogBuilder, Messages }
import com.intellij.openapi.util.text.StringUtil
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.model.ChallengeDifficulty
import com.wenjunhuang.codeepiphany.hackerrank.model.Contest
import com.wenjunhuang.codeepiphany.hackerrank.settings.{ HackerRankSettings, HackerRankSettingsConfigurable }
import com.wenjunhuang.codeepiphany.model.*
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.{ ChallengeId, ChallengeLanguageId, SolutionId }
import com.wenjunhuang.codeepiphany.model.CodeDojo.HackerRank
import com.wenjunhuang.codeepiphany.services.file.*
import com.wenjunhuang.codeepiphany.services.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import com.wenjunhuang.codeepiphany.utils.implicits.*
import com.wenjunhuang.codeepiphany.utils.template.VelocityUtils
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger

import java.io.File

object challenge {
  def openChallenge[F[_]: Async: Concurrent: HttpClientKeeper: Logger](
    project: Project,
    challengeSlug: String,
    contest: Contest,
    language: Language,
    languageVersion:LanguageVersion
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
      else Some((state.sourceFolder.get, language, state.fileNameTemplate.get, state.codeTemplate.get))
    }.evalOnEDTAny().flatMap {
      case None => Async[F].unit
      case Some((sourceFolder, language, fileNameTemplate, codeTemplate)) =>
        fetchChallengeContentAndOpen(
          project,
          challengeSlug,
          contest,
          language,
          languageVersion,
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
    languageVersion:LanguageVersion,
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
            val trimmedFileName = StringUtil.trim(fileName)
            val file            = new File(File(sourceFolder), s"$trimmedFileName.${language.fileExt}")
            val settings        = ChallengeSettings.getInstance(project)
            settings.findChallengeId(file.getCanonicalPath) match
              case Some(_) =>
                refreshAndFindFileByIoFile(file)
              case None =>
                (
                  saveTextToFile(file, code).flatMap(refreshAndFindFileByIoFile),
                  storeChallengeToDatabase(project, template)
                ).parTupled.map { case (file, (challengeId, challengeLangId)) =>
                  val settings = ChallengeSettings.getInstance(project)
                  settings.addChallenge(
                    file.get.getCanonicalPath,
                    ChallengeSettingsStateItem(challengeId, challengeLangId, HackerRank, language, None)
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
    challenge: ChallengeCodeTemplate
  ): F[(ChallengeId, ChallengeLanguageId)] = {
    val repository = ChallengeRepository.getInstance(project)
    repository.getDSLContextResource.use { client =>
      Async[F].blocking {
        client.transactionResult { trx =>
          val dsl = trx.dsl()
          val challengeRecord = dsl.fetchOne(
            CHALLENGE,
            CHALLENGE.DOJO.eq(CodeDojo.HackerRank.value).and(CHALLENGE.DOJOID.eq(challenge.dojoId))
          ) match {
            case null => dsl.newRecord(CHALLENGE)
            case r    => r
          }
          challengeRecord.setDescription(challenge.description)
          challengeRecord.setDifficulty(challenge.difficulty)
          challengeRecord.setDojo(CodeDojo.HackerRank.value)
          challengeRecord.setDojoid(challenge.dojoId)
          challengeRecord.setSlug(challenge.slug)
          challengeRecord.setTitle(challenge.name)
          challengeRecord.store()

          val hackerRankRecord =
            dsl.fetchOne(HACKERRANK_CHALLENGE, HACKERRANK_CHALLENGE.ID.eq(challengeRecord.getId)) match
              case null =>
                dsl.newRecord(HACKERRANK_CHALLENGE)
              case r => r
          hackerRankRecord.setId(challengeRecord.getId)
          hackerRankRecord.setContest(challenge.contest)
          hackerRankRecord.setContestslug(challenge.contest)
          hackerRankRecord.store()

          val challengeLanguageRecord = dsl.fetchOne(
            CHALLENGE_LANGUAGE,
            CHALLENGE_LANGUAGE.CHALLENGEID
              .eq(challengeRecord.getId)
              .and(CHALLENGE_LANGUAGE.LANGUAGE.eq(challenge.language.value))
              .and(CHALLENGE_LANGUAGE.LANGUAGEVERSION.eq(challenge.languageVersion.version))
          ) match
            case null => dsl.newRecord(CHALLENGE_LANGUAGE)
            case r    => r
          challengeLanguageRecord.setChallengeid(challengeRecord.getId)
          challengeLanguageRecord.setLanguage(challenge.language.value)
          challengeLanguageRecord.setLanguageversion(challenge.languageVersion.version)
          challengeLanguageRecord.setCodetemplate(StringUtil.convertLineSeparators(challenge.getCode))
          challengeLanguageRecord.store()

          val hackerRankLangRecord = dsl.fetchOne(
            HACKERRANK_CHALLENGE_LANGUAGE,
            HACKERRANK_CHALLENGE_LANGUAGE.ID.eq(challengeLanguageRecord.getId)
          ) match
            case null => dsl.newRecord(HACKERRANK_CHALLENGE_LANGUAGE)
            case r    => r
          hackerRankLangRecord.setId(challengeLanguageRecord.getId)
          hackerRankLangRecord.setCodeheader(StringUtil.convertLineSeparators(challenge.header))
          hackerRankLangRecord.setCodetemplate(StringUtil.convertLineSeparators(challenge.template))
          hackerRankLangRecord.setCodetail(StringUtil.convertLineSeparators(challenge.tail))
          hackerRankLangRecord.store()

          (ChallengeId(challengeRecord.getId), ChallengeLanguageId(challengeLanguageRecord.getId))
        }
      }
    }
  }

}
