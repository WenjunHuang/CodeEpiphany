package com.wenjunhuang.codeepiphany.codeforces

import cats.effect.IO
import com.intellij.openapi.project.Project
import com.intellij.util.ui.ListTableModel
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.OpenChallengeProvider
import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesOpenChallengeService
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettings
import com.wenjunhuang.codeepiphany.database.tables.records.CodeforcesProblemsetsRecord
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import org.typelevel.log4cats.LoggerFactory

import javax.swing.ListSelectionModel

package object ui {

  def createCodeForcesChallengeProvider(
    project: Project,
    selectionModel: ListSelectionModel,
    tableModel: ListTableModel[CodeforcesProblemsetsRecord]
  ): OpenChallengeProvider = {

    new OpenChallengeProvider {
      override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
        selectionModel.getSelectedIndices.toList match
          case head :: _ =>
            val selected = tableModel.getItem(head)
            openChallenge(project, language, languageVersion, selected).unsafeRunAndForget()
          case _ => ()
      }

      override def getLanguages: List[(Language, LanguageVersion)] = {
        CodeForcesSettings.getInstance(project).getSelectedLanguages
      }

      override def currentSelectedCanBeOpened: Boolean = true

    }
  }

  def openChallenge(
    project: Project,
    language: Language,
    languageVersion: LanguageVersion,
    selected: CodeforcesProblemsetsRecord
  ): IO[Unit] = {
    val logger = LoggerFactory.getLogger[IO]
    CodeForcesOpenChallengeService(project)
      .openChallenge(selected, language, languageVersion)
      .handleErrorWith { e =>
        console.showConsole(project) *>
          console.error(project, e.getMessage) *> logger.warn(e)("Failed to open challenge")
      }
      .evalAsBackgroundProgress(project, s"Opening challenge ${selected.getName}...")
  }
}
