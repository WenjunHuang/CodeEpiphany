package com.wenjunhuang.codeepiphany.codeforces

import cats.effect.IO
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project
import com.intellij.ui.SingleSelectionModel
import com.intellij.util.ui.ListTableModel

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.OpenChallengeProvider
import com.wenjunhuang.codeepiphany.codeforces.services.CodeForcesOpenChallengeService
import com.wenjunhuang.codeepiphany.codeforces.settings.CodeForcesSettings
import com.wenjunhuang.codeepiphany.database.tables.records.CodeforcesProblemsetsRecord
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.console.showConsole
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

package object ui {

  def createCodeForcesChallengeProvider(
    project: Project,
    selectionModel: SingleSelectionModel,
    tableModel: ListTableModel[CodeforcesProblemsetsRecord]
  ): OpenChallengeProvider = {
    implicit val httpClientManager: HttpClientManager[IO] = HttpClientService.getInstance(project).httpClientManager
    val logger                                            = LoggerFactory.getLogger[IO]

    new OpenChallengeProvider {
      override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
        selectionModel.getSelectedIndices.toList match
          case head :: _ =>
            val selected = tableModel.getItem(head)
            CodeForcesOpenChallengeService[IO](project)
              .openChallenge(selected, language, languageVersion)
              .handleErrorWith { e =>
                showConsole[IO](project) *>
                  console.error[IO](project, e.getMessage) *> logger.warn(e)("Failed to open challenge")
              }
              .evalAsBackgroundProgress(project, s"Opening challenge ${selected.getName}...")
              .unsafeRunAndForget()
          case _ => ()
      }

      override def getLanguages: List[(Language, LanguageVersion)] = {
        CodeForcesSettings.getInstance(project).getSelectedLanguages
      }

      override def currentSelectedCanBeOpened: Boolean = true

    }
  }
}
