package com.wenjunhuang.codeepiphany.leetcode

import cats.effect.IO
import org.typelevel.log4cats.LoggerFactory

import com.intellij.openapi.project.Project
import com.intellij.ui.SingleSelectionModel
import com.intellij.util.ui.ListTableModel

import com.wenjunhuang.codeepiphany.actions.OpenChallengeActionGroup.OpenChallengeProvider
import com.wenjunhuang.codeepiphany.leetcode.models.LeetCodeChallengeListItem
import com.wenjunhuang.codeepiphany.leetcode.services.{LeetCodeOpenChallengeRequest, LeetCodeOpenChallengeService}
import com.wenjunhuang.codeepiphany.leetcode.settings.{LeetCodeCNSettings, LeetCodeSettings}
import com.wenjunhuang.codeepiphany.model.{CodeDojo, Language, LanguageVersion}
import com.wenjunhuang.codeepiphany.services.console
import com.wenjunhuang.codeepiphany.services.console.showConsole
import com.wenjunhuang.codeepiphany.services.http.{HttpClientManager, HttpClientService}
import com.wenjunhuang.codeepiphany.utils.extensions.*
import com.wenjunhuang.codeepiphany.utils.implicits.*

package object ui {
  def createLeetCodeChallengeProvider(
    project: Project,
    selectionModel: SingleSelectionModel,
    tableModel: ListTableModel[LeetCodeChallengeListItem],
    bootstrap: LeetCodeBootstrapParameters,
    leetCodeDojo: CodeDojo.LeetCode.type | CodeDojo.LeetCodeCN.type
  ): OpenChallengeProvider = {
    implicit val httpClientManager: HttpClientManager[IO] = HttpClientService.getInstance(project).httpClientManager
    val logger                     = LoggerFactory.getLogger[IO]

    new OpenChallengeProvider {
      override def openCurrentSelectedChallenge(language: Language, languageVersion: LanguageVersion): Unit = {
        selectionModel.getSelectedIndices.toList match
          case head :: _ =>
            val selected = tableModel.getItem(head)
            LeetCodeOpenChallengeService[IO](project, leetCodeDojo)
              .openChallenge(LeetCodeOpenChallengeRequest(selected.titleSlug), language, languageVersion)
              .handleErrorWith { e =>
                showConsole[IO](project) *>
                  console.error[IO](project, e.getMessage) *> logger.warn(e)("Failed to open challenge")
              }
              .evalAsBackgroundProgress(project, s"Opening challenge ${selected.title}...")
              .unsafeRunAndForget()
          case _ => ()
      }

      override def getLanguages: List[(Language, LanguageVersion)] = {
        leetCodeDojo match
          case CodeDojo.LeetCode   => LeetCodeSettings.getInstance(project).getSelectedLanguages
          case CodeDojo.LeetCodeCN => LeetCodeCNSettings.getInstance(project).getSelectedLanguages
      }

      override def currentSelectedCanBeOpened: Boolean = {
        selectionModel.getSelectedIndices.toList match
          case head :: _ =>
            val selected = tableModel.getItem(head)
            if selected.paidOnly then bootstrap.userInfo.isPremium.getOrElse(false)
            else true
          case _ => false
      }
    }
  }
}
