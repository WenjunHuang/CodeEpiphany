package com.wenjunhuang.codeepiphany.hackerrank

import cats.effect.IO
import com.intellij.openapi.project.Project
import com.intellij.util.ui.ListTableModel
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.OpenChallengeProvider
import com.wenjunhuang.codeepiphany.hackerrank.models.{HackerRankChallengeDetail, HackerRankContest}
import com.wenjunhuang.codeepiphany.hackerrank.services.{HackerRankOpenChallengeRequest, HackerRankOpenChallengeService}
import com.wenjunhuang.codeepiphany.hackerrank.settings.HackerRankSettings
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import org.typelevel.ci.CIString
import org.typelevel.log4cats.LoggerFactory

import javax.swing.ListSelectionModel

package object ui {
  def createHackerRankChallengeProvider(
    project: Project,
    selectionModel: ListSelectionModel,
    tableModel: ListTableModel[HackerRankChallengeDetail]
  ): OpenChallengeProvider = {
    val logger                                            = LoggerFactory.getLogger[IO]
    new OpenChallengeProvider {
      override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
        selectionModel.getSelectedIndices.toList match
          case first :: _ =>
            val selected = tableModel.getItem(first)
            HackerRankOpenChallengeService(project)
              .openChallenge(
                HackerRankOpenChallengeRequest(
                  selected.slug,
                  HackerRankContest.fromCIString(CIString(selected.contestSlug)).get
                ),
                language,
                languageVersion
              )
              .handleErrorWith(e =>
                console.showConsole(project) *>
                console.error(project, e.getMessage) *>
                logger.warn(e)(s"Failed to open challenge ${selected.slug}")
              )
              .evalAsBackgroundProgressCancellable(project, s"Opening HackerRank challenge '${selected.name}'...")
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
