package com.wenjunhuang.codeepiphany.editor.actions

import cats.effect.IO

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, PlatformCoreDataKeys}
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.{Key, TextRange}

import com.wenjunhuang.codeepiphany.editor.actions.SurroundSubmissionRegionAction.{SURROUND_PROVIDER_KEY, SurroundSubmissionRegionProvider}
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.actions.ActionCompatible
import com.wenjunhuang.codeepiphany.utils.implicits.*

class SurroundSubmissionRegionAction extends AnAction with ActionCompatible{
  override def actionPerformed(e: AnActionEvent): Unit = {
    getProvider(e) match
      case Some(provider) if provider.canSurround => provider.surround()
      case _                                      =>
  }

  override def update(e: AnActionEvent): Unit = {
    getProvider(e) match
      case None           => e.getPresentation.setEnabled(false)
      case Some(provider) => e.getPresentation.setEnabled(provider.canSurround)
  }

  private def getProvider(e: AnActionEvent): Option[SurroundSubmissionRegionProvider] = {
    Option(e.getData(PlatformCoreDataKeys.FILE_EDITOR)).flatMap { editor =>
      Option(SURROUND_PROVIDER_KEY.get(editor))
    }
  }
}

object SurroundSubmissionRegionAction {
  val SURROUND_PROVIDER_KEY: Key[SurroundSubmissionRegionProvider] =
    Key[SurroundSubmissionRegionProvider]("SurroundSubmissionRegionProvider")

  trait SurroundSubmissionRegionProvider {
    def surround(): Unit

    def canSurround: Boolean
  }

  def createProvider(editor: EditorEx, project: Project): SurroundSubmissionRegionProvider =
    new SurroundSubmissionRegionProvider {
      override def surround(): Unit = {
        ChallengeSettings.getInstance(project).findChallengeId(editor.getVirtualFile.getCanonicalPath) match
          case Some(challenge) =>
            IO.delay {
              val selection = editor.getSelectionModel
              if selection.hasSelection then
                val start      = selection.getSelectionStart
                val end        = selection.getSelectionEnd
                val code       = editor.getDocument.getText(TextRange(start, end))
                val surrounded = challenge.language.encloseCodeInRegion(code)
                WriteCommandAction.runWriteCommandAction(
                  project,
                  new Runnable {
                    override def run(): Unit = {
                      editor.getDocument.replaceString(start, end, surrounded)
                    }
                  }
                )
            }.evalOnEDTWithWrite()
              .unsafeRunAndForget()
          case _ =>
      }

      override def canSurround: Boolean = {
        ChallengeSettings.getInstance(project).findChallengeId(editor.getVirtualFile.getCanonicalPath) match
          case None    => false
          case Some(_) => editor.getSelectionModel.hasSelection
      }
    }
}
