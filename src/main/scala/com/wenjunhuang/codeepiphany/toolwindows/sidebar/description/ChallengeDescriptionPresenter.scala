package com.wenjunhuang.codeepiphany.toolwindows.sidebar.description

import cats.effect.{ Async, IO }
import cats.effect.kernel.Resource.ExitCase
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Stream
import java.io.File
import java.net.URI
import org.typelevel.log4cats.LoggerFactory
import scala.concurrent.duration.*
import scala.jdk.OptionConverters.*

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
import com.wenjunhuang.codeepiphany.model.{ ChallengeRepository, CodeDojo }
import com.wenjunhuang.codeepiphany.model.ChallengeRepository.ChallengeId
import com.wenjunhuang.codeepiphany.settings.ChallengeSettings
import com.wenjunhuang.codeepiphany.utils.implicits.*

class ChallengeDescriptionPresenter(private val myProject: Project) extends Disposable {
  private val logger            = Logger.getInstance(getClass)
  private val myLogger          = LoggerFactory.getLogger[IO]()
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
              setChallenge(None)
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
          repository
            .getDSLContextResource[IO]
            .use { dsl =>
              IO.blocking {
                dsl
                  .selectFrom(CHALLENGE)
                  .where(CHALLENGE.ID.eq(challengeId.value))
                  .fetchOptional()
                  .toScala
                  .map { _.getDescription }
              }.flatMap { record =>
                IO.delay {
                  record.map { description =>
                    myDescriptionView.setDescription(Some(description, dojo))
                  }
                }.evalOnEDTDefault()
              }.void
            }
            .handleErrorWith { e =>
              myLogger.warn(e)("Error while fetching challenge description")
            }
        case None =>
          IO.delay { myDescriptionView.setDescription(None) }
            .evalOnEDTDefault()
            .handleErrorWith { e =>
              myLogger.warn(e)("Error while fetching challenge description")
            }
      }
      .onFinalizeCase {
        case ExitCase.Canceled =>
          myLogger.debug("Description presenter stream canceled")
        case _ => IO.unit
      }
      .compile
      .drain
  } yield ()).unsafeRunCancelable()

  Disposer.register(myProject, this)

  /** Handle user clicked a link in the description view browser
    */
  def userClickedLink[F[_]: Async](url: String): F[Unit] =
    Async[F].delay(URI.create(url)).flatMap(uri => ChallengeDescriptionPresenter.browseURI(uri, myProject))

  private def setChallenge(challenge: Option[(ChallengeId, CodeDojo)]): Unit = {
    if myQueue.isEmpty then logger.info("Queue is not initialized")
    else
      logger.info("Queue is initialized")
      myQueue.foreach(_.offer(challenge).unsafeRunAndForget())
  }

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
              FileTypeChooser.associateFileType(vf.getName) match
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
