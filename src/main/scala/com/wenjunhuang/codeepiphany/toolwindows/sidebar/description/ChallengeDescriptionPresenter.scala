package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description

import cats.effect.std.Queue
import cats.effect.{ Async, IO }
import cats.syntax.all.*
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.{ FileEditorManager, FileEditorManagerEvent, FileEditorManagerListener }
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.fileTypes.ex.FileTypeChooser
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.io.URLUtil
import com.wenjunhuang.codeepiphany.database.Tables.CHALLENGE
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.ChallengeId
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, CodeDojo }
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.implicits.*
import fs2.Stream

import java.io.File
import java.net.{ URI, URL }
import scala.concurrent.duration.*

class ChallengeDescriptionPresenter(private val myProject: Project) extends Disposable {
  private val logger            = Logger.getInstance(this.getClass)
  private val myDescriptionView = ChallengeDescriptionView(this, myProject)

  myProject.getMessageBus
    .connect(this)
    .subscribe(
      FileEditorManagerListener.FILE_EDITOR_MANAGER,
      new FileEditorManagerListener {
        override def selectionChanged(event: FileEditorManagerEvent): Unit = {
          Option(event.getNewFile) match {
            case Some(vf) =>
              val settings = ChallengeSettings.getInstance(myProject)
              settings.findChallengeId(vf) match
                case Some(challenge) =>
                  setChallenge(Some((ChallengeId(challenge.challengeId), challenge.dojo)))
                case None =>
                  setChallenge(None)
            case None =>
          }
        }
      }
    )

  @volatile
  private var myQueue: Option[Queue[IO, Option[(ChallengeId, CodeDojo)]]] = None
  private val myCancelToken = (for {
    queue <- Queue.unbounded[IO, Option[(ChallengeId, CodeDojo)]]
    _     <- IO.delay { myQueue = Option(queue) }
    _ <- Stream
      .fromQueueUnterminated(queue)
      .debounce(200.millis)
      .evalTap {
        case Some((challengeId, dojo)) =>
          val repository = ChallengeRepository.getInstance(myProject)
          repository.getDSLContextResource[IO].use { dsl =>
            IO.blocking {
              dsl
                .selectFrom(CHALLENGE)
                .where(CHALLENGE.ID.eq(challengeId.value))
                .fetchOptional()
                .map { record =>
                  val description = record.getDescription
                  myDescriptionView.setDescription(Some(description, dojo))
                }
            }
          }
        case None =>
          IO.delay { myDescriptionView.setDescription(None) }
      }
      .compile
      .drain
  } yield ()).unsafeRunCancelable()

  Disposer.register(myProject, this)

  /** Handle user clicked a link in the description view browser
    */
  def userClickedLink[F[_]: Async](url: String): F[Unit] =
    Async[F].delay(URI.create(url)).flatMap(uri => ChallengeDescriptionPresenter.browseURI(uri, myProject))

  private def setChallenge(challenge: Option[(ChallengeId, CodeDojo)]): Unit =
    myQueue.foreach(_.offer(challenge).unsafeRunAndForget())

  def getView: ChallengeDescriptionView = myDescriptionView

  override def dispose(): Unit = {
    logger.debug("DescriptionPresenter disposed")
    myCancelToken()
  }
}

object ChallengeDescriptionPresenter {
  def browseURI[F[_]: Async](uri: URI, project: Project): F[Unit] = {
    if uri.getScheme == URLUtil.FILE_PROTOCOL then
      Async[F].delay {
        val file = File(uri)
        if !file.exists() || file.isDirectory then throw new IllegalArgumentException(s"Invalid file: $file")
        file
      }.flatMap { file =>
        Async[F].delay {
          val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
          FileEditorManager.getInstance(project).openFile(vf, false) match
            case null | Array() =>
              FileTypeChooser.getKnownFileTypeOrAssociate(vf, project) match
                case null | FileTypes.UNKNOWN =>
                case _ =>
                  FileEditorManager.getInstance(project).openFile(vf, false)
            case _ =>
        }.void.evalOnEDTAny()
      }
    else
      Async[F].delay {
        BrowserUtil.browse(uri.toURL)
      }
  }
}
