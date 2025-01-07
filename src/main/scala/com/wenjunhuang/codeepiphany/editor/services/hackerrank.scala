package com.wenjunhuang.codeepiphany.editor.services

import cats.effect.{ Async, Concurrent }
import cats.effect.kernel.Resource.ExitCase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{ VirtualFile, VirtualFileUtil }
import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.hackerrank.model.Contest
import com.wenjunhuang.codeepiphany.hackerrank.services.HackerRankApi
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, Language }
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.HttpClientKeeper
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings.ChallengeSettingsStateItem
import fs2.Stream
import org.typelevel.ci.CIString

object hackerrank {
  def runCode[F[_]: Async: Concurrent: HttpClientKeeper](
    vf: VirtualFile,
    project: Project,
    item: ChallengeSettingsStateItem
  ): F[Unit] = {
    Stream
      .eval(
        ChallengeRepository
          .getInstance(project)
          .getDSLContextResource[F]
          .use { client =>
            Async[F].blocking {
              Option(
                client
                  .select(
                    CHALLENGE.SLUG,
                    HACKERRANK_CHALLENGE.CONTESTSLUG,
                    CHALLENGE_LANGUAGE.LANGUAGE,
                    CHALLENGE_LANGUAGE.LANGUAGEVERSION
                  )
                  .from(CHALLENGE)
                  .innerJoin(HACKERRANK_CHALLENGE)
                  .on(CHALLENGE.ID.eq(HACKERRANK_CHALLENGE.ID))
                  .innerJoin(CHALLENGE_LANGUAGE)
                  .on(CHALLENGE.ID.eq(CHALLENGE_LANGUAGE.CHALLENGEID))
                  .where(CHALLENGE.ID.eq(item.challengeId).and(CHALLENGE_LANGUAGE.ID.eq(item.challengeLanguageId)))
                  .fetchOne()
              ).flatMap { record =>
                val challengeSlug = record.get(CHALLENGE.SLUG)
                val contestSlug   = record.get(HACKERRANK_CHALLENGE.CONTESTSLUG)
                val language      = record.get(CHALLENGE_LANGUAGE.LANGUAGE)
                val langVer       = record.get(CHALLENGE_LANGUAGE.LANGUAGEVERSION)

                Contest
                  .fromCIString(CIString(contestSlug))
                  .zip(Language.fromCIString(CIString(language)))
                  .map((_, _, langVer, challengeSlug))
              } match {
                case Some(value) => value
                case None        => throw new Exception("Cannot find data for file")
              }
            }
          }
      )
      .flatMap { case (contest, language, langVer, challengeSlug) =>
        val extractedCode = language.extractSubmitCode(VirtualFileUtil.readText(vf))
        HackerRankApi[F]()
          .runAnswer(challengeSlug, contest, language, langVer, extractedCode)
      }
      .onFinalizeCase {
        case ExitCase.Succeeded =>
          Async[F].unit
        case ExitCase.Errored(e) =>
          console.error[F](project, s"Error to run code: \n ${e.getMessage}")
        case ExitCase.Canceled =>
          console.warn[F](project, s"Code submission cancelled for ${vf.getCanonicalPath}")
      }
      .last
      .evalTap {
        case Some(response) =>
          response.compilemessage.filter(_.nonEmpty) match
            case Some(message) =>
              console.error[F](project, s"Compilation Error: \n ${message}")
            case None =>
              if response.testcaseStatus.contains(0) then console.error[F](project, "Wrong Answer!")
              else console.info[F](project, "Passed!")
        case None => Async[F].unit
      }
      .compile
      .drain

  }
}
