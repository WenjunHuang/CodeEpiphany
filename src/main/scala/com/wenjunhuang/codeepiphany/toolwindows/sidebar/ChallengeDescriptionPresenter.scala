package com.wenjunhuang.codeepiphany.toolwindows.sidebar

import cats.effect.Async
import cats.effect.syntax.all.*
import cats.syntax.all.*
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.io.URLUtil
import com.wenjunhuang.codeepiphany.model.Repository.ChallengeStorageItem
import com.wenjunhuang.codeepiphany.utils.implicits.*

import java.io.File
import java.net.URL

class ChallengeDescriptionPresenter(private val project: Project) extends Disposable {
  private val logger            = Logger.getInstance(this.getClass)
  private val myDescriptionView = ChallengeChallengeDescriptionView(this)

  Disposer.register(project, this)

  /** Handle user clicked a link in the description view browser
    */
  def userClickedLink[F[_]: Async](url: String): F[Unit] =
    Async[F].delay(URL(url)).flatMap(url => ChallengeDescriptionPresenter.openUrl(url, project))

  def updateCurrentQuestion(question: ChallengeStorageItem): Unit = myDescriptionView.updateCurrentQuestion(question)

  def getView: ChallengeChallengeDescriptionView = myDescriptionView

  override def dispose(): Unit =
    logger.debug("DescriptionPresenter disposed")
}

object ChallengeDescriptionPresenter {
  private def openUrl[F[_]: Async](url: URL, project: Project): F[Unit] =
    if url.getProtocol == URLUtil.FILE_PROTOCOL then
      Async[F].delay {
        val file = File(url.toURI)
        if !file.exists() || file.isDirectory then throw new IllegalArgumentException(s"Invalid file: $file")
        else file
      }.flatMap { file =>
        Async[F].delay {
          val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
          FileEditorManager.getInstance(project).openFile(vf, false) match
            case null | Array() =>
              FileTypeChooser.getKnownFileTypeOrAssociate(vf, project) match
                case null | FileTypes.UNKNOWN => ()
                case _ =>
                  FileEditorManager.getInstance(project).openFile(vf, false)
                  ()
            case _ => ()
        }.evalOnEDTAny()
      }
    else
      Async[F].delay {
        BrowserUtil.browse(url.toExternalForm)
      }
}
