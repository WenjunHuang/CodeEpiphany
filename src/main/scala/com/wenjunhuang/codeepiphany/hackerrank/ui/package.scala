package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.IO
import javax.swing.ListSelectionModel
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project
import com.intellij.util.ui.ListTableModel

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.OpenChallengeProvider
import com.wenjunhuang.codeepiphany.hackerrank.models.{HackerRankChallengeDetail, HackerRankContest}
import com.wenjunhuang.codeepiphany.hackerrank.services.{HackerRankOpenChallengeRequest, HackerRankOpenChallengeService}
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.console.showConsole
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*

package object ui {
  def createHackerRankChallengeProvider(
    project: Project,
    selectionModel: ListSelectionModel,
    tableModel: ListTableModel[HackerRankChallengeDetail]
  ): OpenChallengeProvider = {
    implicit val httpClientManager: HttpClientManager[IO] = HttpClientService.getInstance(project).httpClientManager
    val logger                                            = LoggerFactory.getLogger[IO]
    new OpenChallengeProvider {
      override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
        selectionModel.getSelectedIndices.toList match
          case first :: _ =>
            val selected = tableModel.getItem(first)
            HackerRankOpenChallengeService[IO](project)
              .openChallenge(
                HackerRankOpenChallengeRequest(
                  selected.slug,
                  HackerRankContest.fromCIString(CIString(selected.contestSlug)).get
                ),
                language,
                languageVersion
              )
              .handleErrorWith(e =>
                showConsole[IO](project) *>
                console.error[IO](project, e.getMessage) *>
                logger.warn(e)(s"Failed to open challenge ${selected.slug}")
              )
              .evalAsBackgroundProgress(project, s"Opening HackerRank challenge '${selected.name}'...")
              .unsafeRunAndForget()
          case _ => ()
      }

      override def getLanguages: List[(Language, LanguageVersion)] = {
        val settings = HackerRankSettings.getInstance(project)
        settings.getSelectedLanguages
      }

      override def currentSelectedCanBeOpened: Boolean = true
    }
  }
}
