package com.wenjunhuang.codeepiphany.luogu

import cats.effect.IO
import com.intellij.openapi.project.Project
import com.intellij.util.ui.ListTableModel
import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.OpenChallengeProvider
import com.wenjunhuang.codeepiphany.luogu.models.LuoGuChallengeItem
import com.wenjunhuang.codeepiphany.luogu.services.LuoGuOpenChallengeService
import com.wenjunhuang.codeepiphany.luogu.settings.LuoGuSettings
import com.wenjunhuang.codeepiphany.model.{Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.syntax.*
import org.typelevel.log4cats.LoggerFactory

import javax.swing.ListSelectionModel

package object ui {

  def createLuoGuChallengeProvider(
    project: Project,
    selectionModel: ListSelectionModel,
    tableModel: ListTableModel[LuoGuChallengeItem]
  ): OpenChallengeProvider = {
    val logger = LoggerFactory.getLogger[IO]

    new OpenChallengeProvider {
      override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
        selectionModel.getSelectedIndices.toList match
          case head :: _ =>
            val selected = tableModel.getItem(head)
            LuoGuOpenChallengeService(project)
              .openChallenge(selected, language, languageVersion)
              .handleErrorWith { e =>
                console.showConsole(project) *>
                  console.error(project, e.getMessage) *> logger.warn(e)("Failed to open challenge")
              }
              .evalAsBackgroundProgress(project, s"Opening challenge ${selected.title}...")
              .unsafeRunAndForget()
          case _ => ()
      }

      override def getLanguages: List[(Language, LanguageVersion)] = {
        LuoGuSettings.getInstance(project).getSelectedLanguages
      }

      override def currentSelectedCanBeOpened: Boolean = true

    }
  }
}
