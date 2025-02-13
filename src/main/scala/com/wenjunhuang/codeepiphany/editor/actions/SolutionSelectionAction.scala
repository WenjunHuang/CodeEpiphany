package com.wenjunhuang.codeepiphany.editor.actions

import javax.swing.JComponent
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.{ CheckboxAction, ComboBoxAction }
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

import com.wenjunhuang.codeepiphany.database.Tables.*
import com.wenjunhuang.codeepiphany.editor.actions.SolutionSelectionAction.*
import com.wenjunhuang.codeepiphany.model.newtypes.SolutionId
import com.wenjunhuang.codeepiphany.services.{ AuthService, ChallengeRepository }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.actions.ActionCompatible

class SolutionSelectionAction extends ComboBoxAction with ActionCompatible {

  override def createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup = {
    getProvider(dataContext) match
      case Some(provider) =>
        DefaultActionGroup(
          provider.getSolutionItems
            .map(solution =>
              new CheckboxAction(solution.title) with ActionCompatible {
                override def isSelected(e: AnActionEvent): Boolean =
                  provider.getSelectedSolution.exists(_.solutionId == solution.solutionId)

                override def setSelected(e: AnActionEvent, state: Boolean): Unit =
                  if state then provider.selectSolution(solution)
              }
            )
            .toArray*
        )
      case None => DefaultActionGroup()
  }

  override def update(e: AnActionEvent): Unit = {
    val presentation = e.getPresentation
    getProvider(e.getDataContext) match
      case None =>
        presentation.setEnabled(false)
      case Some(provider) =>
        if provider.enabled then
          presentation.setText(provider.getSelectedSolution.map(_.title).getOrElse("Select Solution"))
          presentation.setEnabled(true)
        else presentation.setEnabled(false)
  }

  private def getProvider(dataContext: DataContext): Option[SolutionSelectionProvider] = {
    Option(dataContext.getData(PlatformCoreDataKeys.FILE_EDITOR)).flatMap { editor =>
      Option(SOLUTION_PROVIDER_KEY.get(editor))
    }
  }
}

object SolutionSelectionAction {
  final val SOLUTION_PROVIDER_KEY: Key[SolutionSelectionProvider] =
    Key.create[SolutionSelectionProvider]("SOLUTION_PROVIDER_KEY")
  case class SolutionActionItem(solutionId: SolutionId, title: String)

  trait SolutionSelectionProvider {
    def enabled: Boolean
    def getSolutionItems: List[SolutionActionItem]
    def selectSolution(solutionId: SolutionActionItem): Unit
    def getSelectedSolution: Option[SolutionActionItem]
  }

  def createSolutionSelectionProvider(project: Project, vf: VirtualFile): SolutionSelectionProvider =
    new SolutionSelectionProvider:
      override def enabled: Boolean = {
        val challengeSettings = ChallengeSettings.getInstance(project)
        challengeSettings.findChallengeId(vf).exists { challenge =>
          AuthService.getInstance(project).isLoggedIn(challenge.dojo)
        }
      }

      override def getSolutionItems: List[SolutionActionItem] = {
        ChallengeSettings
          .getInstance(project)
          .findChallengeId(vf)
          .map { challengeItem =>
            ChallengeRepository
              .getInstance(project)
              .getDSLContext
              .selectFrom(SOLUTION)
              .where(SOLUTION.CHALLENGEID.eq(challengeItem.challengeId))
              .fetch()
              .asScala
              .map { record =>
                val solutionId = SolutionId(record.getId)
                val title      = record.getTitle
                SolutionActionItem(solutionId, title)
              }
              .toList
          }
          .getOrElse(List.empty)
      }

      override def selectSolution(solutionId: SolutionActionItem): Unit = {
        ChallengeSettings
          .getInstance(project)
          .setChallengeSolutionId(vf, solutionId.solutionId)
      }

      override def getSelectedSolution: Option[SolutionActionItem] = {
        ChallengeSettings
          .getInstance(project)
          .findChallengeId(vf)
          .flatMap { challengeItem =>
            val dsl = ChallengeRepository.getInstance(project).getDSLContext
            dsl
              .select(SOLUTION.ID, SOLUTION.TITLE)
              .from(SOLUTION)
              .where(SOLUTION.ID.eq(challengeItem.solutionId))
              .fetchOptional()
              .toScala
              .map { record =>
                SolutionActionItem(SolutionId(record.get(SOLUTION.ID).longValue()), record.get(SOLUTION.TITLE))
              }
          }
      }
}
